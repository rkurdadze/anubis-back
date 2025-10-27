package ge.comcom.anubis.service.core;

import ge.comcom.anubis.dto.ObjectDto;
import ge.comcom.anubis.entity.core.ObjectClass;
import ge.comcom.anubis.entity.core.ObjectEntity;
import ge.comcom.anubis.entity.core.ObjectLinkEntity;
import ge.comcom.anubis.entity.core.ObjectType;
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
        return objectRepository.save(entity);
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

        objectMapper.updateEntityFromDto(dto, existing);
        applyRelations(existing, dto);

        log.info("Updating object ID {}", id);
        return objectRepository.save(existing);
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
        return objectRepository.findByIsDeletedFalse();
    }

    /**
     * Retrieves object by ID.
     */
    @Transactional(readOnly = true)
    public ObjectEntity getById(Long id) {
        return objectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Object not found: " + id));
    }

    /**
     * Retrieves object with all outgoing and incoming links (using JOIN FETCH).
     */
    @Transactional(readOnly = true)
    public ObjectEntity getWithLinks(Long id) {
        return objectRepository.findByIdWithLinks(id)
                .orElseThrow(() -> new EntityNotFoundException("Object not found: " + id));
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

    /**
     * @deprecated Use {@link #getById(Long)} instead.
     * @see #getById(Long)
     */
    @Deprecated
    public ObjectEntity getObjectById(Long id) {
        return getById(id);
    }
}