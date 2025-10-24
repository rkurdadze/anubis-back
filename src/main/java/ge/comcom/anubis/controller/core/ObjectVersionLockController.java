package ge.comcom.anubis.controller.core;

import ge.comcom.anubis.entity.core.ObjectVersionEntity;
import ge.comcom.anubis.service.core.ObjectVersionLockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller providing endpoints for managing version locking (check-out/check-in)
 * in an M-Files-like repository system.
 * <p>
 * Supports explicit lock/unlock operations, ownership checks, and forced unlocks.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/version-lock")
@RequiredArgsConstructor
@Tag(name = "Version Locking", description = "API for managing object version locks (check-out / check-in operations)")
public class ObjectVersionLockController {

    private final ObjectVersionLockService lockService;

    // ============================================================
    // LOCK
    // ============================================================

    /**
     * Locks (checks out) a specific object version for a given user.
     * <p>
     * If the version is already locked by another user, the operation will fail with an error.
     * </p>
     *
     * Example:
     * <pre>
     * POST /api/v1/version-lock/123/lock/5
     * </pre>
     *
     * @param versionId the version ID to lock
     * @param userId    the user performing the lock
     * @return the locked {@link ObjectVersionEntity}
     */
    @PostMapping("/{versionId}/lock/{userId}")
    @Operation(summary = "Lock version (check-out)",
            description = "Locks a specific object version for editing. "
                    + "If already locked by another user, an error is thrown.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Version locked successfully."),
            @ApiResponse(responseCode = "423", description = "Version is already locked by another user."),
            @ApiResponse(responseCode = "404", description = "Version not found.")
    })
    public ResponseEntity<ObjectVersionEntity> lock(
            @Parameter(description = "Version ID", example = "123") @PathVariable Long versionId,
            @Parameter(description = "User ID performing the lock", example = "5") @PathVariable Long userId) {
        return ResponseEntity.ok(lockService.lock(versionId, userId));
    }

    // ============================================================
    // UNLOCK
    // ============================================================

    /**
     * Unlocks (checks in) a specific object version.
     * <p>
     * By default, only the user who owns the lock can unlock.
     * If {@code force=true}, administrators can override and release the lock forcibly.
     * </p>
     *
     * Example:
     * <pre>
     * POST /api/v1/version-lock/123/unlock/5?force=false
     * </pre>
     *
     * @param versionId the version ID to unlock
     * @param userId    the user performing the unlock
     * @param force     if {@code true}, unlocks even if the version is locked by another user
     * @return the unlocked {@link ObjectVersionEntity}
     */
    @PostMapping("/{versionId}/unlock/{userId}")
    @Operation(summary = "Unlock version (check-in)",
            description = "Unlocks a version (check-in). "
                    + "If the current user does not own the lock, use force=true to override.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Version unlocked successfully."),
            @ApiResponse(responseCode = "423", description = "Cannot unlock: version owned by another user."),
            @ApiResponse(responseCode = "404", description = "Version not found.")
    })
    public ResponseEntity<ObjectVersionEntity> unlock(
            @Parameter(description = "Version ID", example = "123") @PathVariable Long versionId,
            @Parameter(description = "User ID performing the unlock", example = "5") @PathVariable Long userId,
            @Parameter(description = "Force unlock flag (true = override ownership)", example = "false")
            @RequestParam(defaultValue = "false") boolean force) {
        return ResponseEntity.ok(lockService.unlock(versionId, userId, force));
    }

    // ============================================================
    // CHECK LOCK STATUS
    // ============================================================

    /**
     * Checks the lock status of a version for a given user.
     * <p>
     * Returns "Unlocked or owned" if the version is free or owned by the same user.
     * Otherwise returns HTTP 423 Locked with an explanatory message.
     * </p>
     *
     * Example:
     * <pre>
     * GET /api/v1/version-lock/123/check/5
     * </pre>
     *
     * @param versionId version ID to check
     * @param userId    user ID performing the check
     * @return plain text lock status message
     */
    @GetMapping("/{versionId}/check/{userId}")
    @Operation(summary = "Check lock status",
            description = "Verifies whether a version is locked. "
                    + "Returns 'Unlocked or owned' if the current user can edit the version.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Version is unlocked or owned by current user."),
            @ApiResponse(responseCode = "423", description = "Version is locked by another user."),
            @ApiResponse(responseCode = "404", description = "Version not found.")
    })
    public ResponseEntity<String> checkLock(
            @Parameter(description = "Version ID to check", example = "123") @PathVariable Long versionId,
            @Parameter(description = "User ID performing the check", example = "5") @PathVariable Long userId) {
        try {
            lockService.assertUnlockedOrOwned(versionId, userId);
            return ResponseEntity.ok("Unlocked or owned");
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(423).body(ex.getMessage()); // 423 Locked
        }
    }
}
