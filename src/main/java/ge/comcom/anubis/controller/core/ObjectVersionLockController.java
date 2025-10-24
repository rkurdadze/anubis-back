package ge.comcom.anubis.controller.core;

import ge.comcom.anubis.entity.core.ObjectVersionEntity;
import ge.comcom.anubis.service.core.ObjectVersionLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for version lock/unlock (check-out/check-in).
 */
@RestController
@RequestMapping("/api/v1/version-lock")
@RequiredArgsConstructor
public class ObjectVersionLockController {

    private final ObjectVersionLockService lockService;

    @PostMapping("/{versionId}/lock/{userId}")
    public ResponseEntity<ObjectVersionEntity> lock(
            @PathVariable Long versionId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(lockService.lock(versionId, userId));
    }

    @PostMapping("/{versionId}/unlock/{userId}")
    public ResponseEntity<ObjectVersionEntity> unlock(
            @PathVariable Long versionId,
            @PathVariable Long userId,
            @RequestParam(defaultValue = "false") boolean force) {
        return ResponseEntity.ok(lockService.unlock(versionId, userId, force));
    }

    @GetMapping("/{versionId}/check/{userId}")
    public ResponseEntity<String> checkLock(
            @PathVariable Long versionId,
            @PathVariable Long userId) {
        try {
            lockService.assertUnlockedOrOwned(versionId, userId);
            return ResponseEntity.ok("Unlocked or owned");
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(423).body(ex.getMessage()); // 423 Locked
        }
    }
}
