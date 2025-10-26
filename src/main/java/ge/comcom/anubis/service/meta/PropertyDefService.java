package ge.comcom.anubis.service.meta;

import ge.comcom.anubis.dto.PropertyDefDto;
import ge.comcom.anubis.dto.PropertyDefRequest;
import ge.comcom.anubis.entity.core.ObjectType;
import ge.comcom.anubis.entity.core.ValueList;
import ge.comcom.anubis.entity.meta.PropertyDef;
import ge.comcom.anubis.repository.meta.ValueListRepository;
import ge.comcom.anubis.repository.core.ObjectTypeRepository;
import ge.comcom.anubis.repository.meta.PropertyDefRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PropertyDefService {

    private final PropertyDefRepository repository;
    private final ObjectTypeRepository objectTypeRepository;
    private final ValueListRepository valueListRepository;

    public PropertyDefDto create(PropertyDefRequest req) {
        if (repository.existsByNameIgnoreCase(req.getName())) {
            throw new IllegalArgumentException("PropertyDef with name already exists: " + req.getName());
        }

        ObjectType refType = null;
        ValueList vlist = null;

        if (req.getRefObjectTypeId() != null)
            refType = objectTypeRepository.findById(req.getRefObjectTypeId())
                    .orElseThrow(() -> new EntityNotFoundException("ObjectType not found"));

        if (req.getValueListId() != null)
            vlist = valueListRepository.findById(req.getValueListId())
                    .orElseThrow(() -> new EntityNotFoundException("ValueList not found"));

        PropertyDef entity = PropertyDef.builder()
                .name(req.getName().trim())
                .captionI18n(req.getCaptionI18n())
                .dataType(req.getDataType())
                .refObjectType(refType)
                .valueList(vlist)
                .isMultiselect(req.getIsMultiselect() != null ? req.getIsMultiselect() : false)
                .isRequired(req.getIsRequired() != null ? req.getIsRequired() : false)
                .isUnique(req.getIsUnique() != null ? req.getIsUnique() : false)
                .regex(req.getRegex())
                .defaultValue(req.getDefaultValue())
                .description(req.getDescription())
                .build();

        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Page<PropertyDefDto> list(Pageable pageable) {
        // Фильтруем только активные PropertyDef
        List<PropertyDefDto> active = repository.findAllActive()
                .stream()
                .map(this::toDto)
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), active.size());
        return new PageImpl<>(active.subList(start, end), pageable, active.size());
    }


    @Transactional(readOnly = true)
    public PropertyDefDto get(Long id) {
        return toDto(repository.findById(id)
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

        if (req.getCaptionI18n() != null) e.setCaptionI18n(req.getCaptionI18n());
        if (req.getDataType() != null) e.setDataType(req.getDataType());
        if (req.getIsMultiselect() != null) e.setIsMultiselect(req.getIsMultiselect());
        if (req.getIsRequired() != null) e.setIsRequired(req.getIsRequired());
        if (req.getIsUnique() != null) e.setIsUnique(req.getIsUnique());
        if (req.getRegex() != null) e.setRegex(req.getRegex());
        if (req.getDefaultValue() != null) e.setDefaultValue(req.getDefaultValue());
        if (req.getDescription() != null) e.setDescription(req.getDescription());

        if (req.getRefObjectTypeId() != null) {
            e.setRefObjectType(objectTypeRepository.findById(req.getRefObjectTypeId())
                    .orElseThrow(() -> new EntityNotFoundException("ObjectType not found")));
        }

        if (req.getValueListId() != null) {
            e.setValueList(valueListRepository.findById(req.getValueListId())
                    .orElseThrow(() -> new EntityNotFoundException("ValueList not found")));
        }

        return toDto(repository.save(e));
    }

    public void delete(Long id) {
        if (!repository.existsById(id))
            throw new EntityNotFoundException("PropertyDef not found: id=" + id);
        repository.deleteById(id);
    }

    private PropertyDefDto toDto(PropertyDef e) {
        return PropertyDefDto.builder()
                .id(e.getId())
                .name(e.getName())
                .captionI18n(e.getCaptionI18n())
                .dataType(e.getDataType())
                .refObjectTypeId(e.getRefObjectType() != null ? e.getRefObjectType().getId() : null)
                .valueListId(e.getValueList() != null ? e.getValueList().getId() : null)
                .isMultiselect(e.getIsMultiselect())
                .isRequired(e.getIsRequired())
                .isUnique(e.getIsUnique())
                .regex(e.getRegex())
                .defaultValue(e.getDefaultValue())
                .description(e.getDescription())
                .build();
    }

    @Transactional
    public void deactivatePropertyDef(Long id) {
        PropertyDef def = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PropertyDef not found: " + id));
        repository.deactivate(def);
    }

}
