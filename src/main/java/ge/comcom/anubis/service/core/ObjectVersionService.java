package ge.comcom.anubis.service.core;

import ge.comcom.anubis.entity.core.ObjectEntity;
import ge.comcom.anubis.entity.core.ObjectVersionEntity;
import ge.comcom.anubis.enums.VersionChangeType;
import ge.comcom.anubis.repository.core.ObjectRepository;
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
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ObjectVersionService {

    private final ObjectVersionRepository versionRepository;
    private final ObjectRepository objectRepository;
    private final ObjectVersionAuditService auditService;

    /**
     * Saves or updates a version entity.
     */
    public ObjectVersionEntity save(ObjectVersionEntity entity) {
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        log.debug("Saving version for objectId={} versionNumber={}",
                entity.getObject() != null ? entity.getObject().getId() : "null",
                entity.getVersionNumber());

        ObjectVersionEntity saved = versionRepository.save(entity);
        auditService.logAction(
                saved,
                VersionChangeType.VERSION_SAVED,
                null,
                "Version saved/updated by " + entity.getCreatedBy()
        );

        return saved;
    }

    /**
     * Deletes a version entity by ID.
     */
    public void delete(Long id) {
        ObjectVersionEntity version = versionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Version not found with ID: " + id));

        versionRepository.delete(version);
        auditService.logAction(
                version,
                VersionChangeType.VERSION_DELETED,
                null,
                "Version deleted"
        );
        log.warn("Deleted version id={}", id);
    }

    /**
     * Retrieves a version entity by ID.
     */
    @Transactional(readOnly = true)
    public ObjectVersionEntity getById(Long id) {
        return versionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Version not found with ID: " + id));
    }

    /**
     * Retrieves all versions associated with a specific object.
     */
    @Transactional(readOnly = true)
    public List<ObjectVersionEntity> getByObjectId(Long objectId) {
        return versionRepository.findByObject_IdOrderByVersionNumberAsc(objectId);
    }

    /**
     * Automatically creates a new version for a given object.
     */
    public ObjectVersionEntity createNewVersion(Long objectId, String comment) {
        ObjectEntity object = objectRepository.findById(objectId)
                .orElseThrow(() -> new EntityNotFoundException("Object not found: " + objectId));

        Integer lastVersion = versionRepository.findLastVersionNumber(objectId);
        int newVersion = (lastVersion == null ? 1 : lastVersion + 1);

        ObjectVersionEntity entity = ObjectVersionEntity.builder()
                .object(object)
                .versionNumber(newVersion)
                .createdAt(Instant.now())
                .createdBy("system") // TODO: replace with authenticated user
                .comment(comment != null ? comment : "Auto-created version")
                .build();

        ObjectVersionEntity saved = versionRepository.save(entity);
        auditService.logAction(
                saved,
                VersionChangeType.VERSION_CREATED,
                null,
                "Created new version " + newVersion
        );

        log.info("Auto-created version {} for object {}", newVersion, objectId);
        return saved;
    }

    /**
     * Returns the latest version for a given object.
     */
    @Transactional(readOnly = true)
    public ObjectVersionEntity getLatestVersion(Long objectId) {
        return versionRepository
                .findTopByObject_IdOrderByVersionNumberDesc(objectId)
                .orElse(null);
    }

    /**
     * Returns latest version or creates if none exists.
     */
    public ObjectVersionEntity getOrCreateLatestVersion(Long objectId) {
        ObjectVersionEntity latest = getLatestVersion(objectId);
        return (latest != null)
                ? latest
                : createNewVersion(objectId, "Auto-created initial version");
    }
}
