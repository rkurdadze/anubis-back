package ge.comcom.anubis.service.core;

import ge.comcom.anubis.entity.core.ObjectVersionEntity;
import ge.comcom.anubis.repository.core.ObjectVersionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Service layer for managing object versions.
 * Provides methods for creating, retrieving, and deleting version entities.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ObjectVersionService {

    private final ObjectVersionRepository versionRepository;

    /**
     * Saves a new or updated version entity.
     *
     * @param entity ObjectVersionEntity to save
     * @return persisted ObjectVersionEntity
     */
    public ObjectVersionEntity save(ObjectVersionEntity entity) {
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        log.info("Saving version for objectId={} versionNumber={}", entity.getObjectId(), entity.getVersionNumber());
        return versionRepository.save(entity);
    }

    /**
     * Deletes a version entity by ID.
     *
     * @param id ID of the version entity to delete
     */
    public void delete(Long id) {
        if (!versionRepository.existsById(id)) {
            throw new EntityNotFoundException("Version not found with ID: " + id);
        }
        log.warn("Deleting version with id={}", id);
        versionRepository.deleteById(id);
    }

    /**
     * Retrieves a version entity by ID.
     *
     * @param id ID of the version entity
     * @return ObjectVersionEntity
     */
    @Transactional(readOnly = true)
    public ObjectVersionEntity getById(Long id) {
        return versionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Version not found with ID: " + id));
    }

    /**
     * Retrieves all versions associated with a specific object ID.
     *
     * @param objectId ID of the object
     * @return list of ObjectVersionEntity
     */
    @Transactional(readOnly = true)
    public List<ObjectVersionEntity> getByObjectId(Long objectId) {
        return versionRepository.findByObjectIdOrderByVersionNumberAsc(objectId);
    }

    /**
     * Automatically creates a new version for a given object ID.
     * If the object has no previous versions, it starts from version 1.
     *
     * @param objectId ID of the object to version
     * @param comment  optional comment
     * @return newly created ObjectVersionEntity
     */
    public ObjectVersionEntity createNewVersion(Long objectId, String comment) {
        Integer lastVersion = versionRepository.findLastVersionNumber(objectId);
        int newVersion = (lastVersion == null ? 1 : lastVersion + 1);

        ObjectVersionEntity entity = ObjectVersionEntity.builder()
                .objectId(objectId)
                .versionNumber(newVersion)
                .createdAt(Instant.now())
                .createdBy("system") // TODO: Replace with authenticated user
                .comment(comment != null ? comment : "Auto-created version")
                .build();

        log.info("Auto-created version {} for object {}", newVersion, objectId);
        return versionRepository.save(entity);
    }

    /**
     * Retrieves the latest version for a given object.
     *
     * @param objectId ID of the object
     * @return the latest ObjectVersionEntity or null if none exists
     */
    @Transactional(readOnly = true)
    public ObjectVersionEntity getLatestVersion(Long objectId) {
        return versionRepository.findTopByObjectIdOrderByVersionNumberDesc(objectId)
                .orElse(null);
    }
}
