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

            List<Map<String, Object>> filters = parseFilterJson(view.getFilterJson());
            log.debug("✅ Parsed filters ({}): {}", filters.size(), filters);

            if (filters.isEmpty()) {
                throw new IllegalStateException("Invalid or empty filter_json. Please define valid filters before execution.");
            }

            List<ObjectEntity> results = objectRepository.findAll();

            log.debug("Initial object count: {}", results.size());

            // -------------------------------------------------------
            // PROPERTY FILTERS
            // -------------------------------------------------------
            for (Map<String, Object> f : filters) {
                try {
                    if (f.containsKey("propertyDefId") && f.containsKey("value")) {
                        log.debug("Processing property filter: {}", f);

                        Object defRaw = f.get("propertyDefId");
                        Object valRaw = f.get("value");
                        String op = String.valueOf(f.getOrDefault("op", "EQ"));

                        Long defId = Long.valueOf(String.valueOf(defRaw));
                        String value = String.valueOf(valRaw);

                        log.debug("🧩 Applying property filter: defId={}, op={}, value={}", defId, op, value);

                        results = results.stream()
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

                        log.debug("Remaining after property filter: {}", results.size());
                    }
                } catch (Exception e) {
                    log.error("❌ Property filter failed: {} → {}", f, e.getMessage(), e);
                }
            }

            // -------------------------------------------------------
            // LINK FILTERS
            // -------------------------------------------------------
            for (Map<String, Object> f : filters) {
                try {
                    if (f.containsKey("link_role") && f.containsKey("linked_object_id")) {
                        String role = String.valueOf(f.get("link_role"));
                        Long linkedId = Long.valueOf(String.valueOf(f.get("linked_object_id")));
                        log.debug("➡️ Applying link filter: role='{}', linked_object_id={}", role, linkedId);

                        List<ObjectLinkEntity> links = linkRepository.findByTarget_IdAndRole_NameIgnoreCase(linkedId, role);
                        log.debug("Found {} links for role={}", links.size(), role);

                        Set<Long> allowedIds = links.stream()
                                .map(l -> l.getSource().getId())
                                .collect(java.util.stream.Collectors.toSet());

                        results = results.stream()
                                .filter(obj -> allowedIds.contains(obj.getId()))
                                .toList();
                        log.debug("Remaining after direct link filter: {}", results.size());
                    }

                    if (f.containsKey("reverse_link_role") && f.containsKey("reverse_linked_object_id")) {
                        String role = String.valueOf(f.get("reverse_link_role"));
                        Long linkedId = Long.valueOf(String.valueOf(f.get("reverse_linked_object_id")));
                        log.debug("↩️ Applying reverse link filter: role='{}', linked_object_id={}", role, linkedId);

                        List<ObjectLinkEntity> reverseLinks = linkRepository.findBySource_IdAndRole_NameIgnoreCase(linkedId, role);
                        log.debug("Found {} reverse links for role={}", reverseLinks.size(), role);

                        Set<Long> allowedIds = reverseLinks.stream()
                                .map(l -> l.getTarget().getId())
                                .collect(java.util.stream.Collectors.toSet());

                        results = results.stream()
                                .filter(obj -> allowedIds.contains(obj.getId()))
                                .toList();
                        log.debug("Remaining after reverse link filter: {}", results.size());
                    }
                } catch (Exception e) {
                    log.error("❌ Link filter failed: {} → {}", f, e.getMessage(), e);
                }
            }

            log.info("✅ Executed view '{}' (id={}) -> {} result(s)", view.getName(), view.getId(), results.size());
            List<ObjectDto> dtoResults = (results == null || results.isEmpty())
                    ? List.of()
                    : results.stream().map(objectMapper::toDto).toList();
            return dtoResults;

        } catch (Exception e) {
            log.error("🔥 Unhandled error executing view id={} → {}", viewId, e.getMessage(), e);
            throw e;
        }
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


    private List<Map<String, Object>> parseFilterJson(JsonNode filterJson) {
        if (filterJson == null || filterJson.isNull()) {
            log.warn("parseFilterJson(): filterJson is null");
            return List.of();
        }

        try {
            if (filterJson.isArray()) {
                List<Map<String, Object>> list = om.convertValue(filterJson, List.class);
                log.debug("parseFilterJson(): parsed array of {} filters", list.size());
                return list;
            } else if (filterJson.isObject()) {
                Map<String, Object> single = om.convertValue(filterJson, Map.class);
                log.debug("parseFilterJson(): single object -> {}", single);
                return List.of(single);
            } else {
                log.warn("parseFilterJson(): unexpected JSON type: {}", filterJson.getNodeType());
                return List.of();
            }
        } catch (Exception e) {
            log.error("❌ Failed to parse filter_json: {}", e.getMessage(), e);
            return List.of();
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
