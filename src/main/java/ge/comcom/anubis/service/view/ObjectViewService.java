package ge.comcom.anubis.service.view;

import ge.comcom.anubis.dto.ObjectViewDto;
import ge.comcom.anubis.dto.ViewGroupingDto;
import ge.comcom.anubis.entity.core.ObjectEntity;
import ge.comcom.anubis.entity.core.ObjectLinkEntity;
import ge.comcom.anubis.entity.meta.PropertyDef;
import ge.comcom.anubis.entity.security.User;
import ge.comcom.anubis.entity.view.ObjectViewEntity;
import ge.comcom.anubis.entity.view.ObjectViewGroupingEntity;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for managing saved views (virtual folders).
 */
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

    /**
     * Creates a new view.
     */
    public ObjectViewEntity create(ObjectViewDto dto) {
        User creator = userRepository.findById(dto.getCreatedById())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + dto.getCreatedById()));

        ObjectViewEntity view = ObjectViewEntity.builder()
                .name(dto.getName())
                .isCommon(Boolean.TRUE.equals(dto.getIsCommon()))
                .createdBy(creator)
                .filterJson(dto.getFilterJson())
                .sortOrder(dto.getSortOrder())
                .build();

        viewRepository.save(view);

        saveGroupings(view, dto.getGroupings());
        log.info("Created view '{}' (id={})", dto.getName(), view.getId());
        return view;
    }

    /**
     * Updates existing view.
     */
    public ObjectViewEntity update(Long id, ObjectViewDto dto) {
        ObjectViewEntity view = viewRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("View not found: " + id));

        view.setName(dto.getName());
        view.setIsCommon(dto.getIsCommon());
        view.setFilterJson(dto.getFilterJson());
        view.setSortOrder(dto.getSortOrder());

        groupingRepository.deleteAll(groupingRepository.findAllByView_IdOrderByLevelAsc(view.getId()));
        saveGroupings(view, dto.getGroupings());

        log.info("Updated view id={}", id);
        return viewRepository.save(view);
    }

    /**
     * Deletes a view.
     */
    public void delete(Long id) {
        if (!viewRepository.existsById(id)) {
            throw new EntityNotFoundException("View not found: " + id);
        }
        viewRepository.deleteById(id);
        log.warn("Deleted view id={}", id);
    }

    /**
     * Returns all available (own + shared) views.
     */
    @Transactional(readOnly = true)
    public List<ObjectViewEntity> getAvailable(Long userId) {
        return viewRepository.findAllByCreatedBy_IdOrIsCommonTrue(userId);
    }

    /**
     * Internal helper to store groupings.
     */
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



    /**
     * Executes a saved view, taking into account both property and relationship filters
     * including reverse (target-based) relationship filters.
     *
     * Example filter_json:
     * [
     *   {"link_role": "Customer", "linked_object_id": 42},
     *   {"reverse_link_role": "Customer", "reverse_linked_object_id": 42}
     * ]
     */
    public List<ObjectEntity> executeView(Long viewId) {
        ObjectViewEntity view = viewRepository.findById(viewId)
                .orElseThrow(() -> new EntityNotFoundException("View not found: " + viewId));

        List<Map<String, Object>> filters = parseFilterJson(view.getFilterJson());
        List<ObjectEntity> results = objectRepository.findAll();

        for (Map<String, Object> f : filters) {
            // 🔹 Прямые связи (source → target)
            if (f.containsKey("link_role") && f.containsKey("linked_object_id")) {
                String role = (String) f.get("link_role");
                Long linkedId = ((Number) f.get("linked_object_id")).longValue();

                List<ObjectLinkEntity> links = linkRepository.findByTarget_IdAndRole_NameIgnoreCase(linkedId, role);
                results = results.stream()
                        .filter(obj -> links.stream()
                                .anyMatch(l -> l.getSource().getId().equals(obj.getId())))
                        .toList();
            }

            // 🔹 Обратные связи (target ← source)
            if (f.containsKey("reverse_link_role") && f.containsKey("reverse_linked_object_id")) {
                String role = (String) f.get("reverse_link_role");
                Long linkedId = ((Number) f.get("reverse_linked_object_id")).longValue();

                List<ObjectLinkEntity> reverseLinks = linkRepository.findBySource_IdAndRole_NameIgnoreCase(linkedId, role);
                results = results.stream()
                        .filter(obj -> reverseLinks.stream()
                                .anyMatch(l -> l.getTarget().getId().equals(obj.getId())))
                        .toList();
            }
        }

        log.info("Executed view {} with {} results", view.getName(), results.size());
        return results;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseFilterJson(Object filterJson) {
        if (filterJson instanceof List<?>) {
            return (List<Map<String, Object>>) filterJson;
        }
        return List.of();
    }
}
