package ge.comcom.anubis.service.meta;


import ge.comcom.anubis.dto.ClassDto;
import ge.comcom.anubis.dto.ClassRequest;
import ge.comcom.anubis.entity.core.ObjectClass;
import ge.comcom.anubis.entity.core.ObjectType;
import ge.comcom.anubis.entity.security.Acl;
import ge.comcom.anubis.mapper.meta.ObjectClassMapper;
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
    private final ObjectClassMapper mapper;

    public ClassDto create(ClassRequest req) {
        ObjectType type = objectTypeRepository.findById(req.getObjectTypeId())
                .orElseThrow(() -> new EntityNotFoundException("ObjectType not found: id=" + req.getObjectTypeId()));

        String name = req.getName().trim();
        if (classRepository.existsByObjectTypeIdAndNameIgnoreCase(req.getObjectTypeId(), name)) {
            throw new IllegalArgumentException("Class name already exists for this ObjectType");
        }

        Acl acl = null;
        if (req.getAclId() != null) {
            acl = aclRepository.findById(req.getAclId())
                    .orElseThrow(() -> new EntityNotFoundException("ACL not found: id=" + req.getAclId()));
        }

        req.setName(name);
        ObjectClass entity = mapper.toEntity(req);
        entity.setObjectType(type);
        entity.setAcl(acl);

        ObjectClass saved = classRepository.save(entity);
        return mapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<ClassDto> list(Pageable pageable) {
        return classRepository.findAll(pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public ClassDto get(Long id) {
        ObjectClass entity = classRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Class not found: id=" + id));
        return mapper.toDto(entity);
    }

    public ClassDto update(Long id, ClassRequest req) {
        ObjectClass entity = classRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Class not found: id=" + id));

        if (req.getName() != null && !req.getName().isBlank()) {
            String name = req.getName().trim();
            if (classRepository.existsByObjectTypeIdAndNameIgnoreCaseAndIdNot(entity.getObjectType().getId(), name, id)) {
                throw new IllegalArgumentException("Class name already exists for this ObjectType");
            }
            req.setName(name);
        }

        mapper.updateEntityFromRequest(req, entity);

        if (req.getAclId() != null) {
            Acl acl = aclRepository.findById(req.getAclId())
                    .orElseThrow(() -> new EntityNotFoundException("ACL not found: id=" + req.getAclId()));
            entity.setAcl(acl);
        }

        ObjectClass updated = classRepository.save(entity);
        return mapper.toDto(updated);
    }

    public void delete(Long id) {
        if (!classRepository.existsById(id)) {
            throw new EntityNotFoundException("Class not found: id=" + id);
        }
        classRepository.deleteById(id);
    }

}
