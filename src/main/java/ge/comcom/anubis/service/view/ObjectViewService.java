package ge.comcom.anubis.service.view;

import com.fasterxml.jackson.databind.JsonNode;
import ge.comcom.anubis.dto.ObjectDto;
import ge.comcom.anubis.dto.ObjectViewDto;
import ge.comcom.anubis.dto.ViewGroupingDto;
import ge.comcom.anubis.entity.core.ObjectEntity;
import ge.comcom.anubis.entity.core.ObjectLinkEntity;
import ge.comcom.anubis.entity.meta.PropertyDef;
import ge.comcom.anubis.entity.security.User;
import ge.comcom.anubis.entity.view.ObjectViewEntity;
import ge.comcom.anubis.entity.view.ObjectViewGroupingEntity;
import ge.comcom.anubis.mapper.ObjectMapper;
import ge.comcom.anubis.mapper.view.ObjectViewMapper;
import ge.comcom.anubis.repository.core.ObjectLinkRepository;
import ge.comcom.anubis.repository.core.ObjectRepository;
import ge.comcom.anubis.repository.meta.PropertyDefRepository;
import ge.comcom.anubis.repository.security.UserRepository;
import ge.comcom.anubis.repository.view.ObjectViewGroupingRepository;
import ge.comcom.anubis.repository.view.ObjectViewRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ObjectViewService {

    private final ObjectViewRepository viewRepository;
    private final ObjectViewGroupingRepository groupingRepository;
    private final UserRepository userRepository;
    private final PropertyDefRepository propertyDefRepository;
    private final ObjectRepository objectRepository;
    private final ObjectLinkRepository linkRepository;
    private final ObjectMapper objectMapper;
    private final ObjectViewMapper objectViewMapper;
    private final com.fasterxml.jackson.databind.ObjectMapper om;

    public ObjectViewDto create(ObjectViewDto dto) {
        User creator = userRepository.findById(dto.getCreatedById())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + dto.getCreatedById()));

        ObjectViewEntity entity = objectViewMapper.toEntity(dto);
        entity.setCreatedBy(creator);

        // ✅ корректно парсим JSON перед сохранением
        entity.setFilterJson(parseJsonSafely(dto.getFilterJson()));


        ObjectViewEntity saved = viewRepository.save(entity);
        saveGroupings(saved, dto.getGroupings());

        log.info("✅ Created view '{}' (id={})", saved.getName(), saved.getId());
        log.debug("Saved JSONB: {}", saved.getFilterJson());
        return objectViewMapper.toDto(saved);
    }

    public ObjectViewDto update(Long id, ObjectViewDto dto) {
        ObjectViewEntity view = viewRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("View not found: " + id));

        view.setName(dto.getName());
        view.setIsCommon(dto.getIsCommon());
        view.setSortOrder(dto.getSortOrder());

        // ✅ корректно парсим и сохраняем как настоящий JSONB
        view.setFilterJson(parseJsonSafely(dto.getFilterJson()));


        groupingRepository.deleteAll(groupingRepository.findAllByView_IdOrderByLevelAsc(id));
        saveGroupings(view, dto.getGroupings());

        ObjectViewEntity updated = viewRepository.save(view);
        log.info("♻️ Updated view '{}' (id={})", updated.getName(), updated.getId());
        log.debug("Updated JSONB: {}", updated.getFilterJson());
        return objectViewMapper.toDto(updated);
    }


    public void delete(Long id) {
        if (!viewRepository.existsById(id)) {
            throw new EntityNotFoundException("View not found: " + id);
        }
        viewRepository.deleteById(id);
        log.warn("🗑️ Deleted view id={}", id);
    }

    @Transactional(readOnly = true)
    public List<ObjectViewDto> getAvailable(Long userId) {
        List<ObjectViewEntity> views = viewRepository.findAllByCreatedBy_IdOrIsCommonTrue(userId);
        log.info("📋 Loaded {} available views for user {}", views.size(), userId);
        return objectViewMapper.toDtoList(views);
    }

    private void saveGroupings(ObjectViewEntity view, List<ViewGroupingDto> groupingDtos) {
        if (groupingDtos == null || groupingDtos.isEmpty()) return;

        List<ObjectViewGroupingEntity> entities = new ArrayList<>();
        for (ViewGroupingDto g : groupingDtos) {
            PropertyDef def = propertyDefRepository.findById(g.getPropertyDefId())
                    .orElseThrow(() -> new EntityNotFoundException("PropertyDef not found: " + g.getPropertyDefId()));

            ObjectViewGroupingEntity e = ObjectViewGroupingEntity.builder()
                    .view(view)
                    .level(g.getLevel())
                    .propertyDef(def)
                    .build();
            entities.add(e);
        }
        groupingRepository.saveAll(entities);
    }


    public List<ObjectDto> executeView(Long viewId) {
        log.info("▶️ Executing view id={}", viewId);
        try {
            ObjectViewEntity view = viewRepository.findById(viewId)
                    .orElseThrow(() -> new EntityNotFoundException("View not found: " + viewId));

            log.debug("Loaded view '{}', filter_json={}", view.getName(), view.getFilterJson());

            if (view.getFilterJson() == null || view.getFilterJson().isNull()) {
                throw new IllegalStateException("View has no defined filters (filter_json is missing).");
            }

            List<ObjectEntity> allObjects = objectRepository.findAll();
            log.debug("Initial object count: {}", allObjects.size());

            List<ObjectEntity> filtered = applyCompoundFilter(view.getFilterJson(), allObjects);
            log.info("✅ Executed view '{}' (id={}) -> {} result(s)", view.getName(), view.getId(), filtered.size());
            List<ObjectDto> dtoResults = (filtered == null || filtered.isEmpty())
                    ? List.of()
                    : filtered.stream().map(objectMapper::toDto).toList();
            return dtoResults;
        } catch (Exception e) {
            log.error("🔥 Unhandled error executing view id={} → {}", viewId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Рекурсивно применяет compound-фильтры (AND/OR) к списку объектов.
     */
    private List<ObjectEntity> applyCompoundFilter(JsonNode node, List<ObjectEntity> input) {
        if (node == null || node.isNull()) return input;
        // Если это массив условий — применяем каждое как AND
        if (node.isArray()) {
            List<ObjectEntity> current = input;
            for (JsonNode sub : node) {
                current = applyCompoundFilter(sub, current);
            }
            return current;
        }
        // Если это объект с conditions (compound)
        if (node.isObject() && node.has("conditions")) {
            String operator = node.has("operator") ? node.get("operator").asText("AND") : "AND";
            List<JsonNode> subConditions = new ArrayList<>();
            node.get("conditions").forEach(subConditions::add);
            if ("AND".equalsIgnoreCase(operator)) {
                List<ObjectEntity> current = input;
                for (JsonNode sub : subConditions) {
                    current = applyCompoundFilter(sub, current);
                }
                return current;
            } else if ("OR".equalsIgnoreCase(operator)) {
                Set<ObjectEntity> resultSet = new LinkedHashSet<>();
                for (JsonNode sub : subConditions) {
                    List<ObjectEntity> subResult = applyCompoundFilter(sub, input);
                    resultSet.addAll(subResult);
                }
                return new ArrayList<>(resultSet);
            } else {
                log.warn("Unknown compound operator: {}", operator);
                return input;
            }
        }
        // Если это простое условие
        if (node.isObject()) {
            // Property filter
            if (node.has("propertyDefId") && node.has("value")) {
                try {
                    Long defId = node.get("propertyDefId").asLong();
                    String op = node.has("op") ? node.get("op").asText("EQ") : "EQ";
                    String value = node.get("value").asText();
                    return input.stream()
                            .filter(obj -> obj.getVersions() != null && obj.getVersions().stream()
                                    .flatMap(v -> Optional.ofNullable(v.getPropertyValues()).stream().flatMap(Collection::stream))
                                    .anyMatch(pv -> {
                                        if (!pv.getPropertyDef().getId().equals(defId)) return false;
                                        String propValue = String.valueOf(pv.getValueText());
                                        return switch (op.toUpperCase()) {
                                            case "EQ", "=" -> Objects.equals(propValue, value);
                                            case "NEQ", "!=" -> !Objects.equals(propValue, value);
                                            case "LIKE" -> propValue != null && propValue.contains(value);
                                            case "GT" -> compareNumeric(propValue, value) > 0;
                                            case "LT" -> compareNumeric(propValue, value) < 0;
                                            case "GE" -> compareNumeric(propValue, value) >= 0;
                                            case "LE" -> compareNumeric(propValue, value) <= 0;
                                            case "ISNULL" -> propValue == null || propValue.isBlank();
                                            default -> false;
                                        };
                                    }))
                            .toList();
                } catch (Exception e) {
                    log.error("❌ Property filter failed: {} → {}", node, e.getMessage(), e);
                    return input;
                }
            }
            // Link filter
            if (node.has("link_role") && node.has("linked_object_id")) {
                try {
                    String role = node.get("link_role").asText();
                    Long linkedId = node.get("linked_object_id").asLong();
                    List<ObjectLinkEntity> links = linkRepository.findByTarget_IdAndRole_NameIgnoreCase(linkedId, role);
                    Set<Long> allowedIds = links.stream()
                            .map(l -> l.getSource().getId())
                            .collect(java.util.stream.Collectors.toSet());
                    return input.stream()
                            .filter(obj -> allowedIds.contains(obj.getId()))
                            .toList();
                } catch (Exception e) {
                    log.error("❌ Link filter failed: {} → {}", node, e.getMessage(), e);
                    return input;
                }
            }
            // Reverse link filter
            if (node.has("reverse_link_role") && node.has("reverse_linked_object_id")) {
                try {
                    String role = node.get("reverse_link_role").asText();
                    Long linkedId = node.get("reverse_linked_object_id").asLong();
                    List<ObjectLinkEntity> reverseLinks = linkRepository.findBySource_IdAndRole_NameIgnoreCase(linkedId, role);
                    Set<Long> allowedIds = reverseLinks.stream()
                            .map(l -> l.getTarget().getId())
                            .collect(java.util.stream.Collectors.toSet());
                    return input.stream()
                            .filter(obj -> allowedIds.contains(obj.getId()))
                            .toList();
                } catch (Exception e) {
                    log.error("❌ Reverse link filter failed: {} → {}", node, e.getMessage(), e);
                    return input;
                }
            }
        }
        // Если ничего не подошло — возвращаем без изменений
        return input;
    }


    /**
     * Универсальное сравнение числовых строк (для GT/LT/...).
     */
    private int compareNumeric(String a, String b) {
        try {
            double da = Double.parseDouble(a);
            double db = Double.parseDouble(b);
            return Double.compare(da, db);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    

    private JsonNode parseJsonSafely(Object raw) {
        if (raw == null) return null;
        try {
            // Если уже JsonNode
            if (raw instanceof JsonNode node) return node;

            // Если приходит строка, пробуем сначала обычный парсинг
            if (raw instanceof String str) {
                String trimmed = str.trim();

                // если строка начинается и заканчивается кавычками — убираем их
                if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
                    trimmed = trimmed.substring(1, trimmed.length() - 1)
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\");
                }

                return om.readTree(trimmed);
            }

            // fallback — valueToTree
            return om.valueToTree(raw);

        } catch (Exception e) {
            log.error("❌ JSON parse failed: {}", e.getMessage(), e);
            return null;
        }
    }


}
