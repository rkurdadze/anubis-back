package ge.comcom.anubis.service.view;

import com.fasterxml.jackson.databind.JsonNode;
import ge.comcom.anubis.entity.core.ObjectVersionEntity;
import ge.comcom.anubis.entity.view.ObjectViewEntity;
import ge.comcom.anubis.repository.view.ObjectViewRepository;
import ge.comcom.anubis.service.core.FullTextSearchService;
import ge.comcom.anubis.service.security.AclResolverService;
import ge.comcom.anubis.service.security.AclService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Executes saved view filter_json and returns matching ObjectVersionEntities.
 * Supports:
 *  - Compound AND/OR filters
 *  - FTS (propertyDefId == 0)
 *  - ACL
 *  - linkRole / reverseLinkRole via EXISTS(object_link)
 *  - Typed properties: value_text / value_number / value_date / value_boolean
 *  - Ref/value-list: ref_object_id / value_list_item_id
 *  - Per-propertyDef LEFT JOIN aliasing: pv_<defId>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ObjectViewExecutionService {

    private final ObjectViewRepository viewRepository;
    private final FullTextSearchService fullTextSearchService;
    private final AclResolverService aclResolverService;
    private final AclService aclService;
    private final ObjectViewService viewService;

    @PersistenceContext
    private EntityManager em;

    public List<ObjectVersionEntity> execute(Long viewId, Long userId) {
        ObjectViewEntity view = viewRepository.findById(viewId)
                .orElseThrow(() -> new IllegalArgumentException("View not found: " + viewId));

        JsonNode filterJson = viewService.parseJsonSafely(view.getFilterJson());
        if (log.isDebugEnabled()) {
            log.debug("Starting execute(): viewId={}, userId={}, filterJson={}", viewId, userId, filterJson);
        }
        if (filterJson == null || filterJson.isNull()) {
            log.warn("View {} has empty filter_json", viewId);
            return List.of();
        }

        // ACL: читаемые ACL-ы пользователя
        Set<Long> readableAcls = aclService.getReadableAclIds(userId);
        if (log.isDebugEnabled()) {
            log.debug("User {} readable ACLs: {}", userId, readableAcls);
        }
        if (readableAcls.isEmpty()) {
            log.info("User {} has no readable ACLs → empty result", userId);
            return List.of();
        }

        // Построить SQL для фильтров (+ параметры + список нужных propertyDefId + FTS)
        FilterBuildResult built = buildCompoundFilterSql(filterJson);
        if (log.isDebugEnabled()) {
            log.debug("Filter SQL: [{}], params: {}, requiredPropertyDefs: {}, fullTextValue: {}",
                    built.conditionSql, built.params, built.requiredPropertyDefs, built.fullTextValue);
        }


        // FTS: если задан, ограничим версии
        Set<Long> ftsIds = null;
        if (built.fullTextValue != null && !built.fullTextValue.isBlank()) {
            ftsIds = fullTextSearchService.findMatchingVersionIds(built.fullTextValue);
            if (ftsIds.isEmpty()) {
                log.info("FTS returned no results for '{}'", built.fullTextValue);
                return List.of();
            }
        }

        // Базовый SELECT
        StringBuilder sql = new StringBuilder("""
            SELECT DISTINCT v.version_id
            FROM object_version v
            JOIN "object" o ON o.object_id = v.object_id
            """);

        // Для каждого уникального propertyDefId — свой JOIN
        for (Long defId : built.requiredPropertyDefs) {
            String alias = pvAlias(defId);
            String paramDef = "def_join_" + defId;
            sql.append("LEFT JOIN property_value ").append(alias)
                    .append(" ON ").append(alias).append(".object_version_id = v.version_id ")
                    .append("AND ").append(alias).append(".property_def_id = :").append(paramDef).append("\n");
            built.params.put(paramDef, defId);
        }

        sql.append("WHERE 1=1\n");

        // Ограничение по FTS, если есть
        if (ftsIds != null && !ftsIds.isEmpty()) {
            sql.append("AND v.version_id = ANY(:ftsIds)\n");
            built.params.put("ftsIds", ftsIds.toArray(new Long[0]));
        }

        // Условия из фильтра, если есть
        if (!built.conditionSql.isBlank()) {
            sql.append("AND (").append(built.conditionSql).append(")\n");
        }

        if (log.isDebugEnabled()) {
            log.debug("Final SQL for view {}: \n{}", viewId, sql.toString());
        }

        Query query = em.createNativeQuery(sql.toString());
        built.params.forEach(query::setParameter);

        @SuppressWarnings("unchecked")
        List<Number> versionIdsRaw = query.getResultList();
        if (log.isDebugEnabled()) {
            log.debug("Found {} version ids before ACL filtering", versionIdsRaw.size());
        }
        if (versionIdsRaw.isEmpty()) {
            log.debug("No matches before ACL filtering");
            return List.of();
        }

        Set<Long> versionIds = new HashSet<>();
        for (Number n : versionIdsRaw) versionIds.add(n.longValue());

        if (log.isDebugEnabled()) {
            log.debug("Filtering by ACL: readableAcls.size={}, versionIds.size={}", readableAcls.size(), versionIds.size());
        }

        // ACL разрешения на версии
        Map<Long, Long> effectiveAcls = aclResolverService.resolveEffectiveAclIds(versionIds);
        Set<Long> allowedVersions = new LinkedHashSet<>();
        for (Map.Entry<Long, Long> e : effectiveAcls.entrySet()) {
            if (readableAcls.contains(e.getValue())) {
                allowedVersions.add(e.getKey());
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("After ACL filtering: allowedVersions.size={}", allowedVersions.size());
        }
        if (allowedVersions.isEmpty()) {
            log.info("User {} has no readable versions after ACL filtering", userId);
            return List.of();
        }

        // Финальный выбор
        Query q2 = em.createNativeQuery("""
            SELECT * FROM object_version
            WHERE version_id = ANY(:ids)
            """, ObjectVersionEntity.class);
        q2.setParameter("ids", allowedVersions.toArray(new Long[0]));

        @SuppressWarnings("unchecked")
        List<ObjectVersionEntity> result = q2.getResultList();

        log.info("✅ Executed view {} for user {} → {} version(s)", viewId, userId, result.size());
        return result;
    }

    /* ================== INTERNAL: SQL builder ================== */

    private static class FilterBuildResult {
        String conditionSql = "";
        final Map<String, Object> params = new LinkedHashMap<>();
        final Set<Long> requiredPropertyDefs = new LinkedHashSet<>();
        String fullTextValue;
    }

    private static String pvAlias(Long defId) { return "pv_" + defId; }

    private FilterBuildResult buildCompoundFilterSql(JsonNode node) {
        FilterBuildResult res = new FilterBuildResult();
        if (node == null || node.isNull()) return res;

        // Массив → считаем как AND-группу
        if (node.isArray()) {
            List<String> parts = new ArrayList<>();
            for (JsonNode sub : node) {
                FilterBuildResult s = buildCompoundFilterSql(sub);
                if (!s.conditionSql.isBlank()) parts.add(s.conditionSql);
                res.params.putAll(s.params);
                res.requiredPropertyDefs.addAll(s.requiredPropertyDefs);
                if (s.fullTextValue != null) res.fullTextValue = s.fullTextValue;
            }
            res.conditionSql = joinIfNotEmpty("AND", parts);
            if (log.isDebugEnabled()) {
                log.debug("buildCompoundFilterSql: AND-group with {} subconditions", parts.size());
            }
            return res;
        }

        // Compound-объект { operator, conditions }
        if (node.isObject() && node.has("conditions")) {
            String operator = node.has("operator") ? node.get("operator").asText("AND").toUpperCase() : "AND";
            if (!operator.equals("AND") && !operator.equals("OR")) operator = "AND";

            List<String> parts = new ArrayList<>();
            for (JsonNode sub : node.get("conditions")) {
                FilterBuildResult s = buildCompoundFilterSql(sub);
                if (!s.conditionSql.isBlank()) parts.add(s.conditionSql);
                res.params.putAll(s.params);
                res.requiredPropertyDefs.addAll(s.requiredPropertyDefs);
                if (s.fullTextValue != null) res.fullTextValue = s.fullTextValue;
            }
            res.conditionSql = joinIfNotEmpty(operator, parts);
            if (log.isDebugEnabled()) {
                log.debug("buildCompoundFilterSql: {}-group with {} subconditions", operator, parts.size());
            }
            return res;
        }

        // Лист: поддерживаем linkRole / reverseLinkRole / FTS / property
        if (node.isObject()) {
            // 1) Связи
            if (node.has("linkRole") && node.has("value")) {
                String role = node.get("linkRole").asText();
                long targetId = node.get("value").asLong();
                String pRole = "role_" + uuid8();
                String pId = "target_" + uuid8();

                String existsSql = """
                    EXISTS (
                      SELECT 1 FROM object_link l
                      WHERE l.source_id = o.object_id
                        AND LOWER(l.role_name) = LOWER(:%s)
                        AND l.target_id = :%s
                    )
                    """.formatted(pRole, pId).trim();

                res.conditionSql = "(" + existsSql + ")";
                res.params.put(pRole, role);
                res.params.put(pId, targetId);
                if (log.isDebugEnabled()) {
                    log.debug("buildCompoundFilterSql: linkRole condition (role={}, targetId={})", role, targetId);
                }
                return res;
            }

            if (node.has("reverseLinkRole") && node.has("value")) {
                String role = node.get("reverseLinkRole").asText();
                long sourceId = node.get("value").asLong();
                String pRole = "role_" + uuid8();
                String pId = "source_" + uuid8();

                String existsSql = """
                    EXISTS (
                      SELECT 1 FROM object_link l
                      WHERE l.target_id = o.object_id
                        AND LOWER(l.role_name) = LOWER(:%s)
                        AND l.source_id = :%s
                    )
                    """.formatted(pRole, pId).trim();

                res.conditionSql = "(" + existsSql + ")";
                res.params.put(pRole, role);
                res.params.put(pId, sourceId);
                if (log.isDebugEnabled()) {
                    log.debug("buildCompoundFilterSql: reverseLinkRole condition (role={}, sourceId={})", role, sourceId);
                }
                return res;
            }

            // 2) FTS
            if (node.has("propertyDefId") && node.get("propertyDefId").asLong() == 0L && node.has("value")) {
                String val = asTextSafe(node.get("value"));
                if (val != null && !val.isBlank()) res.fullTextValue = val;
                if (log.isDebugEnabled()) {
                    log.debug("buildCompoundFilterSql: FTS condition (value={})", val);
                }
                return res; // условие в SQL не добавляем
            }

            // 3) Свойства (включая ref/value-list)
            if (node.has("propertyDefId") && node.has("value")) {
                long defId = node.get("propertyDefId").asLong();
                String op = node.has("op") ? node.get("op").asText("EQ").toUpperCase() : "EQ";
                String dataType = node.has("dataType") ? node.get("dataType").asText().toLowerCase() : null;

                // Подготовить алиас и пометить, что нужен JOIN
                String alias = pvAlias(defId);
                res.requiredPropertyDefs.add(defId);

                // Определить тип (приоритет: dataType, затем по значению)
                ValueKind kind = (dataType != null) ? parseKindFromDataType(dataType)
                        : inferKindFromValue(node.get("value"));

                // Построить условие и параметры
                PropertyCondition cond = buildPropertyCondition(alias, op, kind, node.get("value"));

                res.conditionSql = "(" + cond.sql + ")";
                res.params.putAll(cond.params);
                if (log.isDebugEnabled()) {
                    log.debug("buildCompoundFilterSql: property condition (defId={}, op={}, kind={}, value={})", defId, op, kind, node.get("value"));
                }
                return res;
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("buildCompoundFilterSql: unrecognized node, returning empty condition");
        }
        return res;
    }

    /* ================== helpers ================== */

    private enum ValueKind { TEXT, NUMBER, DATE, BOOLEAN, REF, VALUELIST }

    private static String joinIfNotEmpty(String op, List<String> parts) {
        if (parts == null || parts.isEmpty()) return "";
        if (parts.size() == 1) return parts.get(0);
        return "(" + String.join(" " + op + " ", parts) + ")";
    }

    private static String asTextSafe(JsonNode n) {
        return (n == null || n.isNull()) ? null : n.asText();
    }

    private static String uuid8() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private static ValueKind parseKindFromDataType(String dt) {
        return switch (dt) {
            case "string", "text" -> ValueKind.TEXT;
            case "number", "numeric", "decimal", "integer", "int", "float", "double" -> ValueKind.NUMBER;
            case "date", "datetime", "timestamp" -> ValueKind.DATE;
            case "bool", "boolean" -> ValueKind.BOOLEAN;
            case "ref", "reference", "object" -> ValueKind.REF;
            case "valuelist", "list", "vl" -> ValueKind.VALUELIST;
            default -> ValueKind.TEXT;
        };
    }

    private static ValueKind inferKindFromValue(JsonNode valueNode) {
        if (valueNode == null || valueNode.isNull()) return ValueKind.TEXT;
        if (valueNode.isBoolean()) return ValueKind.BOOLEAN;
        if (valueNode.isNumber()) return ValueKind.NUMBER;

        String s = valueNode.asText();
        if (s == null) return ValueKind.TEXT;

        // boolean
        String ls = s.toLowerCase(Locale.ROOT).trim();
        if (ls.equals("true") || ls.equals("false")) return ValueKind.BOOLEAN;

        // number
        if (s.matches("^[+-]?\\d+(\\.\\d+)?$")) return ValueKind.NUMBER;

        // ISO date / date-time (простая эвристика)
        if (s.matches("^\\d{4}-\\d{2}-\\d{2}([ T]\\d{2}:\\d{2}(:\\d{2})?)?$")) return ValueKind.DATE;

        // ref / value list (явно как long), но без dataType лучше считать TEXT
        return ValueKind.TEXT;
    }

    private static class PropertyCondition {
        final String sql;
        final Map<String, Object> params;

        PropertyCondition(String sql, Map<String, Object> params) {
            this.sql = sql;
            this.params = params;
        }
    }

    private PropertyCondition buildPropertyCondition(String alias, String op, ValueKind kind, JsonNode valueNode) {
        Map<String, Object> params = new LinkedHashMap<>();
        String pVal = "val_" + uuid8();

        return switch (kind) {
            case TEXT -> {
                String val = asTextSafe(valueNode);
                String cond = switch (op) {
                    case "NEQ" -> {
                        params.put(pVal, val);
                        yield alias + ".value_text <> :" + pVal;
                    }
                    case "LIKE" -> {
                        // если нет спецсимволов, добавим %по краям
                        String likeVal = (val != null && (val.contains("%") || val.contains("_"))) ? val : "%" + val + "%";
                        params.put(pVal, likeVal);
                        yield alias + ".value_text ILIKE :" + pVal;
                    }
                    case "EQ" -> {
                        params.put(pVal, val);
                        yield alias + ".value_text = :" + pVal;
                    }
                    default -> {
                        params.put(pVal, val);
                        yield alias + ".value_text = :" + pVal;
                    }
                };
                yield new PropertyCondition(cond, params);
            }
            case NUMBER -> {
                BigDecimal num = parseBigDecimal(valueNode);
                String cond = switch (op) {
                    case "GT" -> alias + ".value_number > :" + pVal;
                    case "GTE" -> alias + ".value_number >= :" + pVal;
                    case "LT" -> alias + ".value_number < :" + pVal;
                    case "LTE" -> alias + ".value_number <= :" + pVal;
                    case "NEQ" -> alias + ".value_number <> :" + pVal;
                    case "EQ" -> alias + ".value_number = :" + pVal;
                    default -> alias + ".value_number = :" + pVal;
                };
                params.put(pVal, num);
                yield new PropertyCondition(cond, params);
            }
            case DATE -> {
                LocalDateTime dt = parseDateTime(valueNode);
                String cond = switch (op) {
                    case "GT" -> alias + ".value_date > :" + pVal;
                    case "GTE" -> alias + ".value_date >= :" + pVal;
                    case "LT" -> alias + ".value_date < :" + pVal;
                    case "LTE" -> alias + ".value_date <= :" + pVal;
                    case "NEQ" -> alias + ".value_date <> :" + pVal;
                    case "EQ" -> alias + ".value_date = :" + pVal;
                    default -> alias + ".value_date = :" + pVal;
                };
                params.put(pVal, dt);
                yield new PropertyCondition(cond, params);
            }
            case BOOLEAN -> {
                Boolean b = parseBoolean(valueNode);
                String cond = switch (op) {
                    case "NEQ" -> alias + ".value_boolean <> :" + pVal;
                    case "EQ" -> alias + ".value_boolean = :" + pVal;
                    default -> alias + ".value_boolean = :" + pVal;
                };
                params.put(pVal, b);
                yield new PropertyCondition(cond, params);
            }
            case REF -> {
                Long refId = parseLong(valueNode);
                String cond = switch (op) {
                    case "NEQ" -> alias + ".ref_object_id <> :" + pVal;
                    case "EQ" -> alias + ".ref_object_id = :" + pVal;
                    default -> alias + ".ref_object_id = :" + pVal;
                };
                params.put(pVal, refId);
                yield new PropertyCondition(cond, params);
            }
            case VALUELIST -> {
                Long itemId = parseLong(valueNode);
                String cond = switch (op) {
                    case "NEQ" -> alias + ".value_list_item_id <> :" + pVal;
                    case "EQ" -> alias + ".value_list_item_id = :" + pVal;
                    default -> alias + ".value_list_item_id = :" + pVal;
                };
                params.put(pVal, itemId);
                yield new PropertyCondition(cond, params);
            }
            default -> {
                // fallback как TEXT
                String val = asTextSafe(valueNode);
                String cond = alias + ".value_text = :" + pVal;
                params.put(pVal, val);
                yield new PropertyCondition(cond, params);
            }
        };
    }

    private static BigDecimal parseBigDecimal(JsonNode n) {
        if (n == null || n.isNull()) return null;
        if (n.isNumber()) return new BigDecimal(n.asText());
        String s = n.asText();
        return (s == null || s.isBlank()) ? null : new BigDecimal(s);
    }

    private static Boolean parseBoolean(JsonNode n) {
        if (n == null || n.isNull()) return null;
        if (n.isBoolean()) return n.asBoolean();
        String s = n.asText();
        if (s == null) return null;
        String ls = s.toLowerCase(Locale.ROOT).trim();
        if (ls.equals("true") || ls.equals("1") || ls.equals("yes")) return true;
        if (ls.equals("false") || ls.equals("0") || ls.equals("no")) return false;
        return null;
    }

    private static Long parseLong(JsonNode n) {
        if (n == null || n.isNull()) return null;
        if (n.isNumber()) return n.asLong();
        String s = n.asText();
        if (s == null || s.isBlank()) return null;
        try { return Long.parseLong(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private static LocalDateTime parseDateTime(JsonNode n) {
        if (n == null || n.isNull()) return null;
        if (n.isNumber()) {
            // epoch millis → seconds? предположим millis:
            long epoch = n.asLong();
            return LocalDateTime.ofEpochSecond(epoch / 1000, 0, java.time.ZoneOffset.UTC);
        }
        String s = n.asText();
        if (s == null || s.isBlank()) return null;

        // Поддержка YYYY-MM-DD и YYYY-MM-DDTHH:mm(:ss)
        try { return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME); }
        catch (DateTimeParseException ignore) {
            try { return LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay(); }
            catch (DateTimeParseException ignore2) {
                // попытка с пробелом вместо T
                try { return LocalDateTime.parse(s.replace(' ', 'T'), DateTimeFormatter.ISO_LOCAL_DATE_TIME); }
                catch (DateTimeParseException e) {
                    return null;
                }
            }
        }
    }
}
