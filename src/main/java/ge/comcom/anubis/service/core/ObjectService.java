package ge.comcom.anubis.service.core;

import ge.comcom.anubis.entity.core.ObjectEntity;
import ge.comcom.anubis.entity.security.User;
import ge.comcom.anubis.repository.core.ObjectRepository;
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
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ObjectService {

    private final ObjectRepository objectRepository;

    /**
     * Creates a new ObjectEntity.
     *
     * @param entity ObjectEntity to create
     * @return persisted entity
     */
    public ObjectEntity create(ObjectEntity entity) {
        // createdAt no longer exists — handled by version entity instead
        entity.setIsDeleted(false);
        log.info("Creating new object '{}'", entity.getName());
        return objectRepository.save(entity);
    }

    /**
     * Updates existing object data.
     *
     * @param id object ID
     * @param updated new state
     * @return updated ObjectEntity
     */
    public ObjectEntity update(Long id, ObjectEntity updated) {
        ObjectEntity existing = objectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Object not found with ID: " + id));

        // Only editable fields should be updated
        existing.setName(updated.getName());
        existing.setObjectType(updated.getObjectType());
        existing.setObjectClass(updated.getObjectClass());
        existing.setAcl(updated.getAcl());

        log.info("Updating object ID {} with new data", id);
        return objectRepository.save(existing);
    }

    /**
     * Performs soft delete (sets isDeleted=true and adds deletedAt timestamp).
     *
     * @param id object ID
     * @param user optional user performing deletion
     */
    public void softDelete(Long id, User user) {
        ObjectEntity entity = objectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Object not found with ID: " + id));

        entity.setIsDeleted(true);
        entity.setDeletedAt(Instant.now());
        entity.setDeletedBy(user);

        objectRepository.save(entity);
        log.warn("Soft-deleted object with ID {} by user {}", id, user != null ? user.getUsername() : "system");
    }

    /**
     * Permanently removes an object from the database.
     *
     * @param id object ID
     */
    public void hardDelete(Long id) {
        if (!objectRepository.existsById(id)) {
            throw new EntityNotFoundException("Object not found with ID: " + id);
        }
        objectRepository.deleteById(id);
        log.warn("Hard-deleted object with ID {}", id);
    }

    /**
     * Returns all non-deleted objects.
     */
    @Transactional(readOnly = true)
    public List<ObjectEntity> getAllActive() {
        return objectRepository.findAll()
                .stream()
                .filter(o -> !Boolean.TRUE.equals(o.getIsDeleted()))
                .toList();
    }

    /**
     * Retrieves object by ID.
     */
    @Transactional(readOnly = true)
    public ObjectEntity getById(Long id) {
        return objectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Object not found with ID: " + id));
    }
}
