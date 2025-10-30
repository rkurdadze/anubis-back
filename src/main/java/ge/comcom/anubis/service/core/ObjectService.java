package ge.comcom.anubis.service.core;

import ge.comcom.anubis.dto.ObjectDto;
import ge.comcom.anubis.entity.core.ObjectClass;
import ge.comcom.anubis.entity.core.ObjectEntity;
import ge.comcom.anubis.entity.core.ObjectLinkEntity;
import ge.comcom.anubis.entity.core.ObjectType;
import ge.comcom.anubis.entity.core.ObjectVersionEntity;
import ge.comcom.anubis.entity.core.VaultEntity;
import ge.comcom.anubis.entity.security.User;
import ge.comcom.anubis.enums.LinkDirection;
import ge.comcom.anubis.mapper.ObjectMapper;
import ge.comcom.anubis.repository.core.ObjectLinkRepository;
import ge.comcom.anubis.repository.core.ObjectRepository;
import ge.comcom.anubis.repository.core.ObjectTypeRepository;
import ge.comcom.anubis.repository.core.ObjectClassRepository;
import ge.comcom.anubis.repository.core.VaultRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Service layer for managing logical repository objects.
 * Handles CRUD operations with soft-delete logic and audit safety.
 * <p>
 * <strong>Link management is delegated to {@link ObjectLinkService}</strong>.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ObjectService {

    private final ObjectRepository objectRepository;
    private final ObjectLinkService linkService;
    private final ObjectLinkRepository linkRepository; // Только для incoming links (если нет в linkService)
    private final ObjectTypeRepository objectTypeRepository;
    private final ObjectClassRepository objectClassRepository;
    private final VaultRepository vaultRepository;
    private final ObjectMapper objectMapper;
    private final ObjectVersionService objectVersionService;
    private final ObjectVersionAuditService auditService;

    /**
     * Creates a new object.
     *
     * @param dto payload describing the object to create
     * @return persisted entity
     */
    public ObjectEntity create(ObjectDto dto) {
        ObjectEntity entity = objectMapper.toEntity(dto);
        applyRelations(entity, dto);
        entity.setIsDeleted(false);
        log.info("Creating new object '{}'", entity.getName());

        ObjectEntity saved = objectRepository.save(entity);

        ObjectVersionEntity initialVersion = objectVersionService.createNewVersion(
                saved.getId(),
                "Initial version after object creation"
        );

        if (saved.getVersions() != null) {
            saved.getVersions().add(initialVersion);
        }

        return saved;
    }

    /**
     * Updates existing object (only editable fields).
     *
     * @param id  object ID
     * @param dto new state
     * @return updated entity
     */
    public ObjectEntity update(Long id, ObjectDto dto) {
        ObjectEntity existing = getById(id);
        ObjectStateSnapshot beforeUpdate = ObjectStateSnapshot.from(existing);

        objectMapper.updateEntityFromDto(dto, existing);
        applyRelations(existing, dto);

        log.info("Updating object ID {}", id);
        ObjectEntity saved = objectRepository.save(existing);

        ObjectVersionEntity newVersion = objectVersionService.createNewVersion(
                saved.getId(),
                "Auto-created version after object update"
        );

        if (saved.getVersions() != null) {
            saved.getVersions().add(newVersion);
        }

        logObjectFieldChanges(beforeUpdate, saved, newVersion);

        return saved;
    }

    /**
     * Performs soft delete (sets isDeleted=true).
     *
     * @param id   object ID
     * @param user user performing deletion (nullable)
     */
    public void softDelete(Long id, User user) {
        ObjectEntity entity = getById(id);
        entity.setIsDeleted(true);
        entity.setDeletedAt(Instant.now());
        entity.setDeletedBy(user);
        objectRepository.save(entity);
        log.warn("Soft-deleted object ID {} by {}", id, user != null ? user.getUsername() : "system");
    }

    /**
     * Permanently removes object from database.
     *
     * @param id object ID
     */
    public void hardDelete(Long id) {
        if (!objectRepository.existsById(id)) {
            throw new EntityNotFoundException("Object not found: " + id);
        }
        objectRepository.deleteById(id);
        log.warn("Hard-deleted object ID {}", id);
    }

    /**
     * Returns all active (non-deleted) objects.
     */
    @Transactional(readOnly = true)
    public List<ObjectEntity> getAllActive() {
        List<ObjectEntity> objects = objectRepository.findByIsDeletedFalse();
        objects.forEach(this::initializeForRead);
        return objects;
    }

    /**
     * Retrieves object by ID.
     */
    @Transactional(readOnly = true)
    public ObjectEntity getById(Long id) {
        ObjectEntity entity = objectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Object not found: " + id));
        initializeForRead(entity);
        return entity;
    }

    /**
     * Retrieves object with all outgoing and incoming links (using JOIN FETCH).
     */
    @Transactional(readOnly = true)
    public ObjectEntity getWithLinks(Long id) {
        ObjectEntity entity = objectRepository.findByIdWithLinks(id)
                .orElseThrow(() -> new EntityNotFoundException("Object not found: " + id));
        initializeForRead(entity);
        return entity;
    }

    // === Делегирование в ObjectLinkService ===

    /**
     * Creates a link (UNI or BI). BI creates reciprocal link.
     */
    public ObjectLinkEntity createLink(Long sourceId, Long targetId, String role, LinkDirection direction) {
        return linkService.createLink(sourceId, targetId, role, direction);
    }

    /**
     * Removes all links between two objects with given role (both directions).
     */
    public void removeLink(Long sourceId, Long targetId, String role) {
        linkService.removeLink(sourceId, targetId, role);
    }

    /**
     * Returns all outgoing links from the object.
     */
    @Transactional(readOnly = true)
    public List<ObjectLinkEntity> getOutgoingLinks(Long objectId) {
        return linkService.getLinks(objectId);
    }

    /**
     * Returns all incoming links to the object.
     */
    @Transactional(readOnly = true)
    public List<ObjectLinkEntity> getIncomingLinks(Long objectId) {
        return linkRepository.findByTarget_Id(objectId);
    }

    private void applyRelations(ObjectEntity entity, ObjectDto dto) {
        entity.setObjectType(resolveType(dto.getTypeId()));
        entity.setObjectClass(resolveClass(dto.getClassId()));
        entity.setVault(resolveVault(dto.getVaultId()));
    }

    private void logObjectFieldChanges(ObjectStateSnapshot before,
                                       ObjectEntity after,
                                       ObjectVersionEntity version) {
        if (before == null || after == null || version == null) {
            return;
        }

        Long actorId = version.getCreatedBy() != null ? version.getCreatedBy().getId() : null;

        if (!equalsSafe(before.name(), after.getName())) {
            auditService.logFieldChange(
                    version,
                    "name",
                    before.name(),
                    after.getName(),
                    actorId,
                    buildSummary("name", before.name(), after.getName())
            );
        }

        if (!equalsSafe(before.typeId(), extractId(after.getObjectType()))) {
            auditService.logFieldChange(
                    version,
                    "objectType",
                    before.typeDisplay(),
                    formatLookup(after.getObjectType()),
                    actorId,
                    buildSummary("objectType", before.typeDisplay(), formatLookup(after.getObjectType()))
            );
        }

        if (!equalsSafe(before.classId(), extractId(after.getObjectClass()))) {
            auditService.logFieldChange(
                    version,
                    "objectClass",
                    before.classDisplay(),
                    formatLookup(after.getObjectClass()),
                    actorId,
                    buildSummary("objectClass", before.classDisplay(), formatLookup(after.getObjectClass()))
            );
        }

        if (!equalsSafe(before.vaultId(), extractId(after.getVault()))) {
            auditService.logFieldChange(
                    version,
                    "vault",
                    before.vaultDisplay(),
                    formatLookup(after.getVault()),
                    actorId,
                    buildSummary("vault", before.vaultDisplay(), formatLookup(after.getVault()))
            );
        }
    }

    private boolean equalsSafe(Object left, Object right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    private Long extractId(ObjectType type) {
        return type != null ? type.getId() : null;
    }

    private Long extractId(ObjectClass objectClass) {
        return objectClass != null ? objectClass.getId() : null;
    }

    private Long extractId(VaultEntity vault) {
        return vault != null ? vault.getId() : null;
    }

    private String formatLookup(ObjectType type) {
        if (type == null) {
            return null;
        }
        return formatDisplay(type.getName(), type.getId());
    }

    private String formatLookup(ObjectClass objectClass) {
        if (objectClass == null) {
            return null;
        }
        return formatDisplay(objectClass.getName(), objectClass.getId());
    }

    private String formatLookup(VaultEntity vault) {
        if (vault == null) {
            return null;
        }
        return formatDisplay(vault.getName(), vault.getId());
    }

    private String formatDisplay(String name, Long id) {
        StringBuilder builder = new StringBuilder();
        if (name != null && !name.isBlank()) {
            builder.append(name.trim());
        }
        if (id != null) {
            if (!builder.isEmpty()) {
                builder.append(" (ID=").append(id).append(')');
            } else {
                builder.append("ID=").append(id);
            }
        }
        return builder.isEmpty() ? null : builder.toString();
    }

    private String buildSummary(String field,
                                String oldValue,
                                String newValue) {
        return String.format("Field '%s' changed from %s to %s",
                field,
                oldValue != null ? "'" + oldValue + "'" : "<null>",
                newValue != null ? "'" + newValue + "'" : "<null>");
    }

    private record ObjectStateSnapshot(String name,
                                       Long typeId,
                                       String typeDisplay,
                                       Long classId,
                                       String classDisplay,
                                       Long vaultId,
                                       String vaultDisplay) {

        static ObjectStateSnapshot from(ObjectEntity entity) {
            if (entity == null) {
                return null;
            }

            ObjectType type = entity.getObjectType();
            ObjectClass objectClass = entity.getObjectClass();
            VaultEntity vault = entity.getVault();

            return new ObjectStateSnapshot(
                    entity.getName(),
                    type != null ? type.getId() : null,
                    type != null ? safeLookupLabel(type.getName(), type.getId()) : null,
                    objectClass != null ? objectClass.getId() : null,
                    objectClass != null ? safeLookupLabel(objectClass.getName(), objectClass.getId()) : null,
                    vault != null ? vault.getId() : null,
                    vault != null ? safeLookupLabel(vault.getName(), vault.getId()) : null
            );
        }

        private static String safeLookupLabel(String name, Long id) {
            StringBuilder builder = new StringBuilder();
            if (name != null && !name.isBlank()) {
                builder.append(name.trim());
            }
            if (id != null) {
                if (!builder.isEmpty()) {
                    builder.append(" (ID=").append(id).append(')');
                } else {
                    builder.append("ID=").append(id);
                }
            }
            return builder.isEmpty() ? null : builder.toString();
        }
    }

    private void initializeForRead(ObjectEntity entity) {
        Hibernate.initialize(entity.getVersions());
        List<ObjectVersionEntity> versions = entity.getVersions();
        if (versions != null && !versions.isEmpty()) {
            ObjectVersionEntity firstVersion = versions.get(0);
            if (firstVersion != null && firstVersion.getCreatedBy() != null) {
                Hibernate.initialize(firstVersion.getCreatedBy());
            }
        }
    }

    private ObjectType resolveType(Long typeId) {
        if (typeId == null) {
            throw new IllegalArgumentException("Object type must be provided");
        }
        return objectTypeRepository.findById(typeId)
                .orElseThrow(() -> new EntityNotFoundException("Object type not found: " + typeId));
    }

    private ObjectClass resolveClass(Long classId) {
        if (classId == null) {
            return null;
        }
        return objectClassRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("Object class not found: " + classId));
    }

    private VaultEntity resolveVault(Long vaultId) {
        if (vaultId == null) {
            return null;
        }
        return vaultRepository.findById(vaultId)
                .orElseThrow(() -> new EntityNotFoundException("Vault not found: " + vaultId));
    }

}