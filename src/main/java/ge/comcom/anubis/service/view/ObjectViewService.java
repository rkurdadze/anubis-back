package ge.comcom.anubis.service.view;

import ge.comcom.anubis.dto.view.ObjectViewDto;
import ge.comcom.anubis.dto.view.ViewGroupingDto;
import ge.comcom.anubis.entity.meta.PropertyDef;
import ge.comcom.anubis.entity.security.User;
import ge.comcom.anubis.entity.view.ObjectViewEntity;
import ge.comcom.anubis.entity.view.ObjectViewGroupingEntity;
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
}
