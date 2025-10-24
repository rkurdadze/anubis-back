package ge.comcom.anubis.service.core;

import ge.comcom.anubis.entity.core.ObjectVersionEntity;
import ge.comcom.anubis.repository.core.ObjectVersionRepository;
import ge.comcom.anubis.repository.security.UserRepository;
import ge.comcom.anubis.util.UserContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Provides M-Files style Check-out / Lock mechanism for object versions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ObjectVersionLockService {

    private final ObjectVersionRepository versionRepository;
    private final UserRepository userRepository;

    /**
     * Locks a version for the given user.
     */
    public ObjectVersionEntity lock(Long versionId, Long userId) {
        ObjectVersionEntity version = versionRepository.findById(versionId)
                .orElseThrow(() -> new EntityNotFoundException("Version not found: " + versionId));

        if (Boolean.TRUE.equals(version.getIsLocked())) {
            if (version.getLockedBy() != null && version.getLockedBy().getId().equals(userId)) {
                log.info("Version {} is already locked by the same user {}", versionId, userId);
                return version;
            }
            throw new IllegalStateException("Version already locked by another user");
        }

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        version.setIsLocked(true);
        version.setLockedBy(user);
        version.setLockedAt(Instant.now());
        versionRepository.save(version);

        log.info("User {} locked version {}", userId, versionId);
        return version;
    }

    /**
     * Unlocks version (owner or forced).
     */
    public ObjectVersionEntity unlock(Long versionId, Long userId, boolean force) {
        ObjectVersionEntity version = versionRepository.findById(versionId)
                .orElseThrow(() -> new EntityNotFoundException("Version not found: " + versionId));

        if (!Boolean.TRUE.equals(version.getIsLocked())) {
            log.info("Version {} is not locked", versionId);
            return version;
        }

        if (!force) {
            if (version.getLockedBy() == null || !version.getLockedBy().getId().equals(userId)) {
                throw new SecurityException("Cannot unlock: not owner of lock");
            }
        }

        version.setIsLocked(false);
        version.setLockedBy(UserContext.getCurrentUser());
        version.setLockedAt(Instant.now());
        versionRepository.save(version);

        log.info("Version {} unlocked by user {} (force={})", versionId, userId, force);
        return version;
    }

    /**
     * Throws if version is locked by another user.
     */
    @Transactional(readOnly = true)
    public void assertUnlockedOrOwned(Long versionId, Long userId) {
        ObjectVersionEntity version = versionRepository.findById(versionId)
                .orElseThrow(() -> new EntityNotFoundException("Version not found: " + versionId));

        if (Boolean.TRUE.equals(version.getIsLocked()) &&
                (version.getLockedBy() == null || !version.getLockedBy().getId().equals(userId))) {
            throw new IllegalStateException("Version is locked by another user");
        }
    }

    /**
     * Periodic cleanup of stale locks older than 24 hours.
     */
    @Scheduled(cron = "0 0 * * * *") // every hour
    public void autoUnlockStaleLocks() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(24));
        int unlocked = versionRepository.unlockOlderThan(cutoff);
        if (unlocked > 0) {
            log.warn("Auto-unlocked {} stale locks older than 24h", unlocked);
        }
    }
}
