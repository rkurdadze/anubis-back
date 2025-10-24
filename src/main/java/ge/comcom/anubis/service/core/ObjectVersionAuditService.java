package ge.comcom.anubis.service.core;

import ge.comcom.anubis.entity.core.ObjectVersionAuditEntity;
import ge.comcom.anubis.entity.core.ObjectVersionEntity;
import ge.comcom.anubis.enums.VersionChangeType;
import ge.comcom.anubis.repository.core.ObjectVersionAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Service for recording and retrieving version audit logs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ObjectVersionAuditService {

    private final ObjectVersionAuditRepository repository;

    /**
     * Logs an action in the audit table.
     *
     * @param version   affected version
     * @param changeType type of change
     * @param modifiedBy user ID (nullable)
     * @param summary   textual summary
     */
    public void logAction(ObjectVersionEntity version,
                          VersionChangeType changeType,
                          Integer modifiedBy,
                          String summary) {

        ObjectVersionAuditEntity record = ObjectVersionAuditEntity.builder()
                .version(version)
                .changeType(changeType)
                .modifiedAt(Instant.now())
                .modifiedBy(modifiedBy)
                .changeSummary(summary)
                .build();

        repository.save(record);
        log.info("Audit logged: {} for versionId={} by user={}",
                changeType, version.getId(), modifiedBy);
    }

    /**
     * Convenience method for logging version creation.
     */
    public void logVersionCreated(ObjectVersionEntity version) {
        logAction(
                version,
                VersionChangeType.VERSION_CREATED,
                null,
                "Created new version " + version.getVersionNumber()
        );
    }

    /**
     * Returns audit log entries for a given version.
     */
    public List<ObjectVersionAuditEntity> getAuditByVersion(Long versionId) {
        return repository.findByVersion_IdOrderByModifiedAtDesc(versionId);
    }
}
