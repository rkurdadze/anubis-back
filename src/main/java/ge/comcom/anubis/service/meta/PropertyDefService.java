package ge.comcom.anubis.service.meta;

import ge.comcom.anubis.dto.PropertyDefDto;
import ge.comcom.anubis.dto.PropertyDefRequest;
import ge.comcom.anubis.entity.core.ObjectClass;
import ge.comcom.anubis.entity.core.ObjectType;
import ge.comcom.anubis.entity.core.ValueList;
import ge.comcom.anubis.entity.meta.ClassProperty;
import ge.comcom.anubis.entity.meta.ClassPropertyId;
import ge.comcom.anubis.entity.meta.PropertyDef;
import ge.comcom.anubis.enums.PropertyDataType;
import ge.comcom.anubis.mapper.PropertyDefMapper;
import ge.comcom.anubis.repository.core.ObjectTypeRepository;
import ge.comcom.anubis.repository.meta.ClassPropertyRepository;
import ge.comcom.anubis.repository.meta.PropertyDefRepository;
import ge.comcom.anubis.repository.meta.ValueListRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PropertyDefService {

    private final PropertyDefRepository repository;
    private final ClassPropertyRepository classPropertyRepository;
    private final ObjectTypeRepository objectTypeRepository;
    private final ValueListRepository valueListRepository;
    private final PropertyDefMapper mapper;

    public PropertyDefDto create(PropertyDefRequest req) {
        String name = req.getName().trim();
        if (repository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("PropertyDef with name already exists: " + name);
        }

        ObjectType refType = null;
        ValueList vlist = null;

        if (req.getRefObjectTypeId() != null)
            refType = objectTypeRepository.findById(req.getRefObjectTypeId())
                    .orElseThrow(() -> new EntityNotFoundException("ObjectType not found"));

        if (req.getValueListId() != null)
            vlist = valueListRepository.findById(req.getValueListId())
                    .orElseThrow(() -> new EntityNotFoundException("ValueList not found"));

        PropertyDef entity = mapper.toEntity(req);
        entity.setName(name);
        entity.setRefObjectType(refType);
        entity.setValueList(vlist);

        return mapper.toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Page<PropertyDefDto> list(Pageable pageable) {
        // Фильтруем только активные PropertyDef
        List<PropertyDefDto> active = repository.findAllActive()
                .stream()
                .map(mapper::toDto)
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), active.size());
        return new PageImpl<>(active.subList(start, end), pageable, active.size());
    }


    @Transactional(readOnly = true)
    public PropertyDefDto get(Long id) {
        return mapper.toDto(repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PropertyDef not found: id=" + id)));
    }

    public PropertyDefDto update(Long id, PropertyDefRequest req) {
        PropertyDef e = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PropertyDef not found: id=" + id));

        if (req.getName() != null && !req.getName().isBlank()) {
            String name = req.getName().trim();
            if (repository.existsByNameIgnoreCaseAndIdNot(name, id))
                throw new IllegalArgumentException("PropertyDef with name already exists");
            e.setName(name);
        }

        mapper.updateEntityFromRequest(req, e);

        if (req.getRefObjectTypeId() != null) {
            e.setRefObjectType(objectTypeRepository.findById(req.getRefObjectTypeId())
                    .orElseThrow(() -> new EntityNotFoundException("ObjectType not found")));
        }

        if (req.getValueListId() != null) {
            e.setValueList(valueListRepository.findById(req.getValueListId())
                    .orElseThrow(() -> new EntityNotFoundException("ValueList not found")));
        }

        return mapper.toDto(repository.save(e));
    }

    public void delete(Long id) {
        if (!repository.existsById(id))
            throw new EntityNotFoundException("PropertyDef not found: id=" + id);
        repository.deleteById(id);
    }

    public void deactivatePropertyDef(Long id) {
        PropertyDef def = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PropertyDef not found: " + id));
        repository.deactivate(def);
    }



    @Transactional
    public PropertyDef findOrCreateDynamic(ObjectClass objectClass, String name,
                                           PropertyDataType dataType, boolean isMulti) {
        if (objectClass == null || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Class and property name are required");
        }

        String normalizedName = normalizeName(name);

        // 🔹 1. Ищем PropertyDef через связь class_property
        Optional<PropertyDef> existing = repository.findByClassIdAndNameIgnoreCase(objectClass.getId(), normalizedName);

        PropertyDef def;
        if (existing.isPresent()) {
            def = existing.get();
        } else {
            log.info("🆕 Создаётся новое свойство '{}' для класса '{}'", normalizedName, objectClass.getName());

            def = new PropertyDef();
            def.setName(normalizedName);
            def.setDataType(dataType);
            def.setIsMultiselect(isMulti);
            def.setIsRequired(false);
            def.setIsUnique(false);
            def.setIsActive(true);
            def = repository.save(def);

            // Создаём связь class_property
            ClassProperty cp = new ClassProperty();
            cp.setId(new ClassPropertyId(objectClass.getId(), def.getId())); // ✅ ВАЖНО
            cp.setObjectClass(objectClass);
            cp.setPropertyDef(def);
            cp.setIsReadonly(false);
            cp.setIsHidden(false);
            cp.setIsActive(true);
            cp.setDisplayOrder(0);
            classPropertyRepository.save(cp);

            log.info("🔗 Добавлено свойство '{}' в класс '{}'", normalizedName, objectClass.getName());
        }

        return def;
    }

    private String normalizeName(String name) {
        if (name == null) return null;
        return name
                .replace("\uFEFF", "") // убираем BOM
                .trim()
                .replaceAll("\\s+", " ");
    }

}
