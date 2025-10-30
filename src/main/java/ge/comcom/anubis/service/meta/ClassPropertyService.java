package ge.comcom.anubis.service.meta;

import ge.comcom.anubis.dto.ClassPropertyDto;
import ge.comcom.anubis.dto.ClassPropertyRequest;
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
    private final ClassPropertyMapper mapper;

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

    public void delete(Long classId, Long propertyDefId) {
        ClassPropertyId key = new ClassPropertyId(classId, propertyDefId);
        if (!classPropertyRepository.existsById(key))
            throw new EntityNotFoundException(
                    "ClassProperty not found: classId=" + classId + ", propertyDefId=" + propertyDefId);
        classPropertyRepository.deleteById(key);
    }

    public void deactivate(Long classId, Long propertyDefId) {
        ClassPropertyId key = new ClassPropertyId(classId, propertyDefId);
        ClassProperty e = classPropertyRepository.findById(key)
                .orElseThrow(() -> new EntityNotFoundException(
                        "ClassProperty not found: classId=" + classId + ", propertyDefId=" + propertyDefId));
        e.setIsActive(false);
        classPropertyRepository.save(e);
    }

}
