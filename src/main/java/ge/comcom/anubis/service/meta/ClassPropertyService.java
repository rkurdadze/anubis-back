package ge.comcom.anubis.service.meta;

import ge.comcom.anubis.dto.ClassPropertyDto;
import ge.comcom.anubis.dto.ClassPropertyRequest;
import ge.comcom.anubis.entity.core.ObjectClass;
import ge.comcom.anubis.entity.meta.ClassProperty;
import ge.comcom.anubis.entity.meta.PropertyDef;
import ge.comcom.anubis.repository.meta.ClassPropertyRepository;
import ge.comcom.anubis.repository.meta.ClassRepository;
import ge.comcom.anubis.repository.meta.PropertyDefRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassPropertyService {

    private final ClassRepository classRepository;
    private final PropertyDefRepository propertyDefRepository;
    private final ClassPropertyRepository classPropertyRepository;

    public ClassPropertyDto create(ClassPropertyRequest req) {
        ObjectClass objClass = classRepository.findById(req.getClassId())
                .orElseThrow(() -> new EntityNotFoundException("Class not found: id=" + req.getClassId()));

        PropertyDef prop = propertyDefRepository.findById(req.getPropertyDefId())
                .orElseThrow(() -> new EntityNotFoundException("PropertyDef not found: id=" + req.getPropertyDefId()));

        if (classPropertyRepository.existsByObjectClassIdAndPropertyDefId(req.getClassId(), req.getPropertyDefId())) {
            throw new IllegalArgumentException("This property is already assigned to the class");
        }

        ClassProperty entity = ClassProperty.builder()
                .objectClass(objClass)
                .propertyDef(prop)
                .isReadonly(req.getIsReadonly() != null ? req.getIsReadonly() : false)
                .isHidden(req.getIsHidden() != null ? req.getIsHidden() : false)
                .displayOrder(req.getDisplayOrder())
                .build();

        return toDto(classPropertyRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<ClassPropertyDto> listByClass(Long classId) {
        return classPropertyRepository.findAllByObjectClassIdOrderByDisplayOrderAsc(classId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClassPropertyDto get(Long id) {
        return toDto(classPropertyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ClassProperty not found: id=" + id)));
    }

    public ClassPropertyDto update(Long id, ClassPropertyRequest req) {
        ClassProperty e = classPropertyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ClassProperty not found: id=" + id));

        if (req.getIsReadonly() != null) e.setIsReadonly(req.getIsReadonly());
        if (req.getIsHidden() != null) e.setIsHidden(req.getIsHidden());
        if (req.getDisplayOrder() != null) e.setDisplayOrder(req.getDisplayOrder());

        return toDto(classPropertyRepository.save(e));
    }

    public void delete(Long id) {
        if (!classPropertyRepository.existsById(id))
            throw new EntityNotFoundException("ClassProperty not found: id=" + id);
        classPropertyRepository.deleteById(id);
    }

    private ClassPropertyDto toDto(ClassProperty e) {
        return ClassPropertyDto.builder()
                .id(e.getId())
                .classId(e.getObjectClass() != null ? e.getObjectClass().getId() : null)
                .propertyDefId(e.getPropertyDef() != null ? e.getPropertyDef().getId() : null)
                .isReadonly(e.getIsReadonly())
                .isHidden(e.getIsHidden())
                .displayOrder(e.getDisplayOrder())
                .build();
    }
}
