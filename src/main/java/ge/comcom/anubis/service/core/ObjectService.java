package ge.comcom.anubis.service.core;

import ge.comcom.anubis.entity.core.LinkRole;
import ge.comcom.anubis.entity.core.ObjectEntity;
import ge.comcom.anubis.entity.core.ObjectLinkEntity;
import ge.comcom.anubis.entity.security.User;
import ge.comcom.anubis.enums.LinkDirection;
import ge.comcom.anubis.repository.core.LinkRoleRepository;
import ge.comcom.anubis.repository.core.ObjectLinkRepository;
import ge.comcom.anubis.repository.core.ObjectRepository;
import ge.comcom.anubis.util.UserContext;
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

    private final ObjectLinkRepository linkRepository;

    private final LinkRoleRepository linkRoleRepository;

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



    /**
     * Returns object with all incoming/outgoing links.
     */
    @Transactional(readOnly = true)
    public ObjectEntity getWithLinks(Long id) {
        ObjectEntity object = objectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Object not found: " + id));

        List<ObjectLinkEntity> outgoing = linkRepository.findBySource_Id(id);
        List<ObjectLinkEntity> incoming = linkRepository.findByTarget_Id(id);

        object.setOutgoingLinks(outgoing);
        object.setIncomingLinks(incoming);

        log.debug("Loaded object {} with {} outgoing and {} incoming links",
                id, outgoing.size(), incoming.size());

        return object;
    }

    /**
     * Creates a new object link and, if BI, creates a reciprocal link automatically.
     */
    public ObjectLinkEntity createLink(Long sourceId, Long targetId, String role, LinkDirection direction) {
        ObjectEntity source = objectRepository.findById(sourceId)
                .orElseThrow(() -> new EntityNotFoundException("Source object not found: " + sourceId));
        ObjectEntity target = objectRepository.findById(targetId)
                .orElseThrow(() -> new EntityNotFoundException("Target object not found: " + targetId));

        LinkRole linkRole = linkRoleRepository.findByNameIgnoreCase(role)
                .orElseThrow(() -> new EntityNotFoundException("Link role not found: " + role));


        // Create primary link
        ObjectLinkEntity link = ObjectLinkEntity.builder()
                .source(source)
                .target(target)
                .role(linkRole)
                .direction(direction)
                .createdAt(Instant.now())
                .createdBy(UserContext.getCurrentUser())
                .build();
        linkRepository.save(link);

        log.info("Created {} link {} → {}", direction, sourceId, targetId);

        // Create reciprocal link automatically if BI
        if (direction == LinkDirection.BI) {
            ObjectLinkEntity reciprocal = ObjectLinkEntity.builder()
                    .source(target)
                    .target(source)
                    .role(linkRole)
                    .direction(direction)
                    .createdAt(Instant.now())
                    .createdBy(UserContext.getCurrentUser())
                    .build();

            linkRepository.save(reciprocal);
            log.info("Created reciprocal BI link {} → {}", targetId, sourceId);
        }

        return link;
    }

    /**
     * Removes both directions of a link if it exists (for BI links).
     */
    public void removeLink(Long sourceId, Long targetId, String role) {
        List<ObjectLinkEntity> links = linkRepository.findBySource_IdOrTarget_Id(sourceId, targetId);
        links.stream()
                .filter(l -> l.getRole().equals(role))
                .forEach(linkRepository::delete);

        log.info("Removed all links for role='{}' between {} and {}", role, sourceId, targetId);
    }

    /**
     * Returns all outgoing links for an object.
     */
    @Transactional(readOnly = true)
    public List<ObjectLinkEntity> getOutgoingLinks(Long objectId) {
        return linkRepository.findBySource_Id(objectId);
    }

    /**
     * Returns all incoming links for an object.
     */
    @Transactional(readOnly = true)
    public List<ObjectLinkEntity> getIncomingLinks(Long objectId) {
        return linkRepository.findByTarget_Id(objectId);
    }
}
