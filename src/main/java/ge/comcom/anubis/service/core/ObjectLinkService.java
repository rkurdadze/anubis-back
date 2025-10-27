package ge.comcom.anubis.service.core;

import ge.comcom.anubis.entity.core.LinkRole;
import ge.comcom.anubis.entity.core.ObjectEntity;
import ge.comcom.anubis.entity.core.ObjectLinkEntity;
import ge.comcom.anubis.enums.LinkDirection;
import ge.comcom.anubis.repository.core.LinkRoleRepository;
import ge.comcom.anubis.repository.core.ObjectLinkRepository;
import ge.comcom.anubis.repository.core.ObjectRepository;
import ge.comcom.anubis.util.UserContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ObjectLinkService {

    private final ObjectLinkRepository linkRepository;
    private final ObjectRepository objectRepository;
    private final LinkRoleRepository linkRoleRepository;

    /**
     * Creates a new link between two objects.
     * If LinkDirection.BI, creates reverse link as well.
     */
    public ObjectLinkEntity createLink(Long srcId, Long dstId, String roleName, LinkDirection direction) {
        ObjectEntity src = objectRepository.findById(srcId)
                .orElseThrow(() -> new EntityNotFoundException("Source object not found: " + srcId));
        ObjectEntity dst = objectRepository.findById(dstId)
                .orElseThrow(() -> new EntityNotFoundException("Target object not found: " + dstId));

        if (srcId.equals(dstId)) {
            throw new IllegalArgumentException("Cannot link object to itself: " + srcId);
        }

        LinkRole role = linkRoleRepository.findByNameIgnoreCase(roleName)
                .orElseThrow(() -> new EntityNotFoundException("Unknown link role: " + roleName));

        // ПРОВЕРКА ДУБЛИКАТА
        if (existsLink(srcId, dstId, role.getId())) {
            throw new IllegalStateException(
                    String.format("Link already exists: %d -> %d with role '%s'", srcId, dstId, roleName)
            );
        }

        ObjectLinkEntity link = ObjectLinkEntity.builder()
                .source(src)
                .target(dst)
                .role(role)
                .direction(direction)
                .createdAt(Instant.now())
                .createdBy(UserContext.getCurrentUser())
                .build();

        try {
            linkRepository.save(link);
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("uk_object_link_unique")) {
                throw new IllegalStateException(
                        "Link already exists: " + srcId + " -> " + dstId + " with role '" + roleName + "'", e
                );
            }
            throw e;
        }

        if (direction == LinkDirection.BI) {
            if (existsLink(dstId, srcId, role.getId())) {
                log.warn("Reverse link already exists: {} -> {} [{}]", dstId, srcId, roleName);
            } else {
                ObjectLinkEntity reverse = ObjectLinkEntity.builder()
                        .source(dst)
                        .target(src)
                        .role(role)
                        .direction(direction)
                        .createdAt(Instant.now())
                        .createdBy(UserContext.getCurrentUser())
                        .build();

                try {
                    linkRepository.save(reverse);
                } catch (DataIntegrityViolationException e) {
                    if (e.getMessage() != null && e.getMessage().contains("uk_object_link_unique")) {
                        log.warn("Reverse link created by another thread (race): {} -> {} [{}]", dstId, srcId, roleName);
                    } else {
                        throw e;
                    }
                }
            }
        }

        log.info("Created {} link [{}] {} -> {}", direction, role.getName(), srcId, dstId);
        return link;
    }

    private boolean existsLink(Long sourceId, Long targetId, Long roleId) {
        return linkRepository.existsBySource_IdAndTarget_IdAndRole_Id(sourceId, targetId, roleId);
    }

    /**
     * Removes all links between given objects with specific role (both directions if BI).
     */
    // В сервисе
    public void removeLink(Long srcId, Long dstId, String roleName) {
        LinkRole role = linkRoleRepository.findByNameIgnoreCase(roleName)
                .orElseThrow(() -> new EntityNotFoundException("Unknown link role: " + roleName));

        int deleted = linkRepository.deleteBidirectional(srcId, dstId, role.getId());

        if (deleted == 0) {
            log.info("No links found to remove: {} <-> {} [{}]", srcId, dstId, roleName);
        } else {
            log.info("Removed {} link(s) [{}] between {} and {}", deleted, roleName, srcId, dstId);
        }
    }

    /**
     * Get all outgoing links for a given object.
     */
    @Transactional(readOnly = true)
    public List<ObjectLinkEntity> getLinks(Long objectId) {
        return linkRepository.findBySource_Id(objectId);
    }
}
