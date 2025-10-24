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

        LinkRole role = linkRoleRepository.findByNameIgnoreCase(roleName)
                .orElseThrow(() -> new EntityNotFoundException("Unknown link role: " + roleName));

        ObjectLinkEntity link = ObjectLinkEntity.builder()
                .source(src)
                .target(dst)
                .role(role)
                .direction(direction)
                .createdAt(Instant.now())
                .createdBy(UserContext.getCurrentUser())
                .build();

        linkRepository.save(link);

        if (direction == LinkDirection.BI) {
            ObjectLinkEntity reverse = ObjectLinkEntity.builder()
                    .source(dst)
                    .target(src)
                    .role(role)
                    .direction(direction)
                    .createdAt(Instant.now())
                    .createdBy(UserContext.getCurrentUser())
                    .build();
            linkRepository.save(reverse);
        }

        log.info("Created {} link [{}] {} -> {}", direction, role.getName(), srcId, dstId);
        return link;
    }

    /**
     * Removes all links between given objects with specific role (both directions if BI).
     */
    public void removeLink(Long srcId, Long dstId, String roleName) {
        LinkRole role = linkRoleRepository.findByNameIgnoreCase(roleName)
                .orElseThrow(() -> new EntityNotFoundException("Unknown link role: " + roleName));

        List<ObjectLinkEntity> links = linkRepository.findBySource_IdOrTarget_Id(srcId, dstId);
        links.stream()
                .filter(l -> l.getRole().getId().equals(role.getId()))
                .forEach(linkRepository::delete);

        log.info("Removed link [{}] {} <-> {}", roleName, srcId, dstId);
    }

    /**
     * Get all outgoing links for a given object.
     */
    @Transactional(readOnly = true)
    public List<ObjectLinkEntity> getLinks(Long objectId) {
        return linkRepository.findBySource_Id(objectId);
    }
}
