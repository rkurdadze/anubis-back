package ge.comcom.anubis.controller.view;

import ge.comcom.anubis.entity.core.ObjectVersionEntity;
import ge.comcom.anubis.service.view.ObjectViewExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for executing saved object views (virtual folders).
 * <p>
 * Executes predefined views with applied ACL (Access Control List) filtering,
 * ensuring that only objects and versions accessible to a specific user
 * are included in the results.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/views")
@RequiredArgsConstructor
@Tag(name = "View Execution", description = "API for executing saved views with ACL-based access filtering")
public class ObjectViewExecutionController {

    private final ObjectViewExecutionService executionService;

    // ============================================================
    // EXECUTE VIEW WITH ACL
    // ============================================================

    /**
     * Executes a saved view with applied ACL (Access Control List) restrictions.
     * <p>
     * This operation behaves like opening a virtual folder in M-Files:
     * it evaluates the view’s filters (properties, relationships, reverse links)
     * and then filters the result set according to the current user’s permissions.
     * </p>
     *
     * Example:
     * <pre>
     * GET /api/v1/views/42/execute/7
     * </pre>
     * will execute the saved view with ID 42 and return only the object versions
     * accessible to user 7.
     *
     * @param id      ID of the saved view to execute
     * @param userId  ID of the user executing the view (for ACL filtering)
     * @return list of {@link ObjectVersionEntity} accessible to the given user
     */
    @GetMapping("/{id}/execute/{userId}")
    @Operation(summary = "Execute view with ACL filtering",
            description = "Executes the specified saved view and returns all object versions "
                    + "visible to the given user, after applying property, relationship, "
                    + "and access control (ACL) filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "View executed successfully. "
                    + "Returns list of accessible object versions."),
            @ApiResponse(responseCode = "403", description = "User has no access to any matching objects."),
            @ApiResponse(responseCode = "404", description = "View not found.")
    })
    public ResponseEntity<List<ObjectVersionEntity>> executeWithAcl(
            @Parameter(description = "View ID to execute", example = "42") @PathVariable("id") Long id,
            @Parameter(description = "User ID for ACL filtering", example = "7") @PathVariable("userId") Long userId) {
        List<ObjectVersionEntity> result = executionService.execute(id, userId);
        return ResponseEntity.ok(result);
    }
}
