package ge.comcom.anubis.service.meta;

import ge.comcom.anubis.dto.ClassPropertyDto;
import ge.comcom.anubis.dto.ClassPropertyRequest;
import ge.comcom.anubis.dto.EffectiveClassPropertyDto;
import ge.comcom.anubis.entity.core.ObjectClass;
import ge.comcom.anubis.entity.meta.ClassProperty;
import ge.comcom.anubis.entity.meta.ClassPropertyId;
import ge.comcom.anubis.entity.meta.PropertyDef;
import ge.comcom.anubis.mapper.meta.ClassPropertyMapper;
import ge.comcom.anubis.repository.meta.ClassPropertyRepository;
import ge.comcom.anubis.repository.meta.ClassRepository;
import ge.comcom.anubis.repository.meta.PropertyDefRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassPropertyService {

    private final ClassRepository classRepository;
    private final PropertyDefRepository propertyDefRepository;
    private final ClassPropertyRepository classPropertyRepository;
    private final ClassPropertyMapper mapper;

    @CacheEvict(value = "class-effective-properties", allEntries = true)
    public ClassPropertyDto create(ClassPropertyRequest req) {
        ObjectClass objClass = classRepository.findById(req.getClassId())
                .orElseThrow(() -> new EntityNotFoundException("Class not found: id=" + req.getClassId()));

        PropertyDef prop = propertyDefRepository.findById(req.getPropertyDefId())
                .orElseThrow(() -> new EntityNotFoundException("PropertyDef not found: id=" + req.getPropertyDefId()));

        ClassPropertyId key = new ClassPropertyId(req.getClassId(), req.getPropertyDefId());

        if (classPropertyRepository.existsById(key)) {
            throw new IllegalArgumentException("This property is already assigned to the class");
        }

        ClassProperty entity = mapper.toEntity(req);
        entity.setId(key);
        entity.setObjectClass(objClass);
        entity.setPropertyDef(prop);

        return mapper.toDto(classPropertyRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<ClassPropertyDto> listByClass(Long classId) {
        return classPropertyRepository.findAllByObjectClass_IdOrderByDisplayOrderAsc(classId)
                .stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClassPropertyDto get(Long classId, Long propertyDefId) {
        ClassPropertyId key = new ClassPropertyId(classId, propertyDefId);
        return mapper.toDto(classPropertyRepository.findById(key)
                .orElseThrow(() -> new EntityNotFoundException(
                        "ClassProperty not found: classId=" + classId + ", propertyDefId=" + propertyDefId)));
    }

    @CacheEvict(value = "class-effective-properties", allEntries = true)
    public ClassPropertyDto update(Long classId, Long propertyDefId, ClassPropertyRequest req) {
        if (!req.getClassId().equals(classId) || !req.getPropertyDefId().equals(propertyDefId)) {
            throw new IllegalArgumentException("Class/property identifiers in payload must match the path parameters");
        }

        ClassPropertyId key = new ClassPropertyId(classId, propertyDefId);
        ClassProperty e = classPropertyRepository.findById(key)
                .orElseThrow(() -> new EntityNotFoundException(
                        "ClassProperty not found: classId=" + classId + ", propertyDefId=" + propertyDefId));

        mapper.updateEntityFromRequest(req, e);

        return mapper.toDto(classPropertyRepository.save(e));
    }

    @CacheEvict(value = "class-effective-properties", allEntries = true)
    public void delete(Long classId, Long propertyDefId) {
        ClassPropertyId key = new ClassPropertyId(classId, propertyDefId);
        if (!classPropertyRepository.existsById(key))
            throw new EntityNotFoundException(
                    "ClassProperty not found: classId=" + classId + ", propertyDefId=" + propertyDefId);
        classPropertyRepository.deleteById(key);
    }

    @CacheEvict(value = "class-effective-properties", allEntries = true)
    public void deactivate(Long classId, Long propertyDefId) {
        ClassPropertyId key = new ClassPropertyId(classId, propertyDefId);
        ClassProperty e = classPropertyRepository.findById(key)
                .orElseThrow(() -> new EntityNotFoundException(
                        "ClassProperty not found: classId=" + classId + ", propertyDefId=" + propertyDefId));
        e.setIsActive(false);
        classPropertyRepository.save(e);
    }

    @Transactional
    @CacheEvict(value = "class-effective-properties", allEntries = true)
    public void activate(Long classId, Long propertyDefId) {
        ClassPropertyId key = new ClassPropertyId(classId, propertyDefId);
        ClassProperty e = classPropertyRepository.findById(key)
                .orElseThrow(() -> new EntityNotFoundException(
                        "ClassProperty not found: classId=" + classId + ", propertyDefId=" + propertyDefId));
        e.setIsActive(true);
        classPropertyRepository.save(e);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "class-effective-properties", key = "#classId")
    public List<EffectiveClassPropertyDto> listEffectiveByClass(Long classId) {
        ObjectClass target = classRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("Class not found: id=" + classId));

        List<ObjectClass> ancestry = buildAncestry(target);
        Set<Long> classIds = ancestry.stream().map(ObjectClass::getId).collect(Collectors.toCollection(LinkedHashSet::new));

        List<ClassProperty> properties = classIds.isEmpty()
                ? List.of()
                : classPropertyRepository.findAllActiveByClassIds(classIds);

        Map<Long, EffectiveClassPropertyDto> effective = new LinkedHashMap<>();
        Map<Long, List<ClassProperty>> byClass = properties.stream()
                .collect(Collectors.groupingBy(cp -> cp.getObjectClass().getId(), LinkedHashMap::new, Collectors.toList()));

        for (ObjectClass level : ancestry) {
            for (ClassProperty property : byClass.getOrDefault(level.getId(), List.of())) {
                EffectiveClassPropertyDto previous = effective.get(property.getPropertyDef().getId());
                EffectiveClassPropertyDto dto = EffectiveClassPropertyDto.builder()
                        .classId(target.getId())
                        .propertyDefId(property.getPropertyDef().getId())
                        .propertyName(property.getPropertyDef().getName())
                        .sourceClassId(level.getId())
                        .sourceClassName(level.getName())
                        .inherited(!Objects.equals(level.getId(), target.getId()))
                        .isReadonly(Boolean.TRUE.equals(property.getIsReadonly()))
                        .isHidden(Boolean.TRUE.equals(property.getIsHidden()))
                        .displayOrder(property.getDisplayOrder())
                        .build();

                if (previous != null) {
                    dto.setOverridesParent(!Objects.equals(previous.getSourceClassId(), level.getId()));
                    dto.setOverriddenClassId(previous.getSourceClassId());
                } else {
                    dto.setOverridesParent(false);
                }

                effective.put(property.getPropertyDef().getId(), dto);
            }
        }

        List<EffectiveClassPropertyDto> result = new ArrayList<>(effective.values());
        result.sort(Comparator
                .comparing(EffectiveClassPropertyDto::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(EffectiveClassPropertyDto::getPropertyName, Comparator.nullsLast(String::compareToIgnoreCase)));
        return result;
    }

    private List<ObjectClass> buildAncestry(ObjectClass target) {
        LinkedList<ObjectClass> chain = new LinkedList<>();
        ObjectClass cursor = target;
        while (cursor != null) {
            chain.addFirst(cursor);
            cursor = cursor.getParent();
        }
        return chain;
    }

}
