package ge.comcom.anubis.service.meta;


import ge.comcom.anubis.dto.ClassDto;
import ge.comcom.anubis.dto.ClassRequest;
import ge.comcom.anubis.dto.ClassTreeNodeDto;
import ge.comcom.anubis.entity.core.ObjectClass;
import ge.comcom.anubis.entity.core.ObjectType;
import ge.comcom.anubis.entity.meta.ClassProperty;
import ge.comcom.anubis.entity.meta.ClassPropertyId;
import ge.comcom.anubis.entity.meta.PropertyDef;
import ge.comcom.anubis.entity.security.Acl;
import ge.comcom.anubis.mapper.meta.ObjectClassMapper;
import ge.comcom.anubis.repository.core.ObjectTypeRepository;
import ge.comcom.anubis.repository.meta.ClassPropertyRepository;
import ge.comcom.anubis.repository.meta.ClassRepository;
import ge.comcom.anubis.repository.security.AclRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service layer for managing Object Classes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ClassService {

    private final ClassRepository classRepository;
    private final ObjectTypeRepository objectTypeRepository;
    private final AclRepository aclRepository;
    private final ObjectClassMapper mapper;

    private final ClassPropertyRepository classPropertyRepository;

    @CacheEvict(value = "class-effective-properties", allEntries = true)
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

        ObjectClass parent = resolveParent(req.getParentClassId(), type, null);

        req.setName(name);
        ObjectClass entity = mapper.toEntity(req);
        entity.setObjectType(type);
        entity.setAcl(acl);
        entity.setParent(parent);

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

    @CacheEvict(value = "class-effective-properties", allEntries = true)
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

        ObjectClass parent = resolveParent(req.getParentClassId(), entity.getObjectType(), entity);
        entity.setParent(parent);

        ObjectClass updated = classRepository.save(entity);
        return mapper.toDto(updated);
    }

    @CacheEvict(value = "class-effective-properties", allEntries = true)
    public void delete(Long id) {
        if (!classRepository.existsById(id)) {
            throw new EntityNotFoundException("Class not found: id=" + id);
        }
        classRepository.deleteById(id);
    }

    @Transactional
    public ObjectClass upsertByName(ObjectType objectType, String className) {
        return classRepository.findByObjectTypeAndNameIgnoreCase(objectType, className)
                .orElseGet(() -> {
                    ObjectClass newClass = new ObjectClass();
                    newClass.setName(className.trim());
                    newClass.setObjectType(objectType);
                    ObjectClass saved = classRepository.save(newClass);
                    log.info("🆕 Создан ObjectClass '{}' для ObjectType '{}'", className, objectType.getName());
                    return saved;
                });
    }

    @Transactional
    @CacheEvict(value = "class-effective-properties", allEntries = true)
    public void ensureClassPropertyBinding(ObjectClass objectClass, PropertyDef propertyDef) {
        if (objectClass == null || propertyDef == null) return;
        boolean exists = classPropertyRepository.existsByObjectClassAndPropertyDef(objectClass, propertyDef);
        if (!exists) {
            ClassProperty link = new ClassProperty();
            link.setId(new ClassPropertyId(objectClass.getId(), propertyDef.getId()));
            link.setObjectClass(objectClass);
            link.setPropertyDef(propertyDef);
            link.setIsActive(true);
            classPropertyRepository.save(link);
            log.info("🔗 Связано свойство '{}' с классом '{}'", propertyDef.getName(), objectClass.getName());
        }
    }

    @Transactional(readOnly = true)
    public List<ClassTreeNodeDto> getHierarchy(Long objectTypeId) {
        List<ObjectClass> classes = objectTypeId != null
                ? classRepository.findAllByObjectTypeId(objectTypeId)
                : classRepository.findAll();

        Map<Long, ClassTreeNodeDto> nodes = new LinkedHashMap<>();
        classes.forEach(c -> nodes.put(c.getId(), mapToTreeNode(c)));

        List<ClassTreeNodeDto> roots = new ArrayList<>();
        for (ObjectClass c : classes) {
            ClassTreeNodeDto node = nodes.get(c.getId());
            ObjectClass parent = c.getParent();
            if (parent != null && nodes.containsKey(parent.getId())) {
                nodes.get(parent.getId()).getChildren().add(node);
            } else {
                roots.add(node);
            }
        }

        roots.forEach(this::sortTreeRecursively);
        roots.sort(Comparator.comparing(ClassTreeNodeDto::getName, Comparator.nullsLast(String::compareToIgnoreCase)));
        return roots;
    }

    private ClassTreeNodeDto mapToTreeNode(ObjectClass entity) {
        return ClassTreeNodeDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .isActive(entity.getIsActive())
                .objectTypeId(entity.getObjectType() != null ? entity.getObjectType().getId() : null)
                .aclId(entity.getAcl() != null ? entity.getAcl().getId() : null)
                .parentClassId(entity.getParent() != null ? entity.getParent().getId() : null)
                .build();
    }

    private void sortTreeRecursively(ClassTreeNodeDto node) {
        node.getChildren().sort(Comparator.comparing(ClassTreeNodeDto::getName, Comparator.nullsLast(String::compareToIgnoreCase)));
        node.getChildren().forEach(this::sortTreeRecursively);
    }

    private ObjectClass resolveParent(Long parentId, ObjectType type, ObjectClass current) {
        if (parentId == null) {
            return null;
        }
        ObjectClass parent = classRepository.findById(parentId)
                .orElseThrow(() -> new EntityNotFoundException("Parent class not found: id=" + parentId));

        if (type != null && parent.getObjectType() != null
                && !Objects.equals(parent.getObjectType().getId(), type.getId())) {
            throw new IllegalArgumentException("Parent class belongs to a different ObjectType");
        }

        if (current != null) {
            ensureNoCycle(parent, current);
        }
        return parent;
    }

    private void ensureNoCycle(ObjectClass parentCandidate, ObjectClass child) {
        ObjectClass cursor = parentCandidate;
        while (cursor != null) {
            if (Objects.equals(cursor.getId(), child.getId())) {
                throw new IllegalArgumentException("Circular class inheritance is not allowed");
            }
            cursor = cursor.getParent();
        }
    }

}
