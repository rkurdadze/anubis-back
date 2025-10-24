package ge.comcom.anubis.service.meta;


import ge.comcom.anubis.dto.meta.ClassDto;
import ge.comcom.anubis.dto.meta.ClassRequest;
import ge.comcom.anubis.entity.core.ObjectClass;
import ge.comcom.anubis.entity.core.ObjectType;
import ge.comcom.anubis.entity.security.Acl;
import ge.comcom.anubis.repository.core.ObjectTypeRepository;
import ge.comcom.anubis.repository.meta.ClassRepository;
import ge.comcom.anubis.repository.security.AclRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for managing Object Classes.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ClassService {

    private final ClassRepository classRepository;
    private final ObjectTypeRepository objectTypeRepository;
    private final AclRepository aclRepository;

    public ClassDto create(ClassRequest req) {
        ObjectType type = objectTypeRepository.findById(req.getObjectTypeId())
                .orElseThrow(() -> new EntityNotFoundException("ObjectType not found: id=" + req.getObjectTypeId()));

        if (classRepository.existsByObjectTypeIdAndNameIgnoreCase(req.getObjectTypeId(), req.getName())) {
            throw new IllegalArgumentException("Class name already exists for this ObjectType");
        }

        Acl acl = null;
        if (req.getAclId() != null) {
            acl = aclRepository.findById(req.getAclId())
                    .orElseThrow(() -> new EntityNotFoundException("ACL not found: id=" + req.getAclId()));
        }

        ObjectClass entity = ObjectClass.builder()
                .objectType(type)
                .acl(acl)
                .name(req.getName().trim())
                .description(req.getDescription())
                .isActive(req.getIsActive() != null ? req.getIsActive() : Boolean.TRUE)
                .build();

        ObjectClass saved = classRepository.save(entity);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<ClassDto> list(Pageable pageable) {
        return classRepository.findAll(pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public ClassDto get(Long id) {
        ObjectClass entity = classRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Class not found: id=" + id));
        return toDto(entity);
    }

    public ClassDto update(Long id, ClassRequest req) {
        ObjectClass entity = classRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Class not found: id=" + id));

        if (req.getName() != null && !req.getName().isBlank()) {
            if (classRepository.existsByObjectTypeIdAndNameIgnoreCaseAndIdNot(entity.getObjectType().getId(), req.getName(), id)) {
                throw new IllegalArgumentException("Class name already exists for this ObjectType");
            }
            entity.setName(req.getName().trim());
        }

        if (req.getDescription() != null)
            entity.setDescription(req.getDescription());

        if (req.getIsActive() != null)
            entity.setIsActive(req.getIsActive());

        if (req.getAclId() != null) {
            Acl acl = aclRepository.findById(req.getAclId())
                    .orElseThrow(() -> new EntityNotFoundException("ACL not found: id=" + req.getAclId()));
            entity.setAcl(acl);
        }

        ObjectClass updated = classRepository.save(entity);
        return toDto(updated);
    }

    public void delete(Long id) {
        if (!classRepository.existsById(id)) {
            throw new EntityNotFoundException("Class not found: id=" + id);
        }
        classRepository.deleteById(id);
    }

    private ClassDto toDto(ObjectClass entity) {
        return ClassDto.builder()
                .id(entity.getId())
                .objectTypeId(entity.getObjectType() != null ? entity.getObjectType().getId() : null)
                .aclId(entity.getAcl() != null ? entity.getAcl().getId() : null)
                .name(entity.getName())
                .description(entity.getDescription())
                .isActive(entity.getIsActive())
                .build();
    }
}
