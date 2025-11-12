package ge.comcom.anubis.controller.core;

import ge.comcom.anubis.dto.ObjectVersionDto;
import ge.comcom.anubis.dto.ObjectViewDto;
import ge.comcom.anubis.entity.core.ObjectVersionEntity;
import ge.comcom.anubis.service.view.ObjectViewExecutionService;
import ge.comcom.anubis.service.view.ObjectViewService;
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
 * REST controller for managing saved object views.
 * <p>
 * Provides endpoints for creating, updating, deleting, retrieving,
 * and executing logical repository views (virtual folders and saved filters),
 * following M-Files-like model.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/views")
@RequiredArgsConstructor
@Tag(name = "Object Views", description = "API for managing saved views, filters, and virtual folders")
public class ObjectViewController {

    private final ObjectViewService service;
    private final ObjectViewExecutionService executionService;

    // ============================================================
    // CRUD OPERATIONS
    // ============================================================

    @PostMapping
    @Operation(summary = "Create new view",
            description = "Creates a new saved view definition (virtual folder or search preset).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "View created successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid view definition payload.")
    })
    public ResponseEntity<ObjectViewDto> create(
            @Parameter(description = "View DTO containing filter and grouping configuration")
            @RequestBody ObjectViewDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update view",
            description = "Updates an existing saved view definition by its ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "View updated successfully."),
            @ApiResponse(responseCode = "404", description = "View not found.")
    })
    public ResponseEntity<ObjectViewDto> update(
            @Parameter(description = "View ID", example = "101") @PathVariable Long id,
            @Parameter(description = "Updated view definition") @RequestBody ObjectViewDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete view",
            description = "Deletes a saved view definition permanently.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "View deleted successfully."),
            @ApiResponse(responseCode = "404", description = "View not found.")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "View ID", example = "101") @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // RETRIEVAL OPERATIONS
    // ============================================================

    @GetMapping("/available/{userId}")
    @Operation(summary = "Get available views for user",
            description = "Returns all personal and shared views accessible by the given user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of available views returned successfully."),
            @ApiResponse(responseCode = "404", description = "User not found or no available views.")
    })
    public ResponseEntity<List<ObjectViewDto>> getAvailable(
            @Parameter(description = "User ID", example = "5") @PathVariable Long userId) {
        return ResponseEntity.ok(service.getAvailable(userId));
    }

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
    public ResponseEntity<List<ObjectVersionDto>> executeWithAcl(
            @Parameter(description = "View ID to execute", example = "42") @PathVariable("id") Long id,
            @Parameter(description = "User ID for ACL filtering", example = "7") @PathVariable("userId") Long userId) {
        List<ObjectVersionDto> result = executionService.execute(id, userId);
        return ResponseEntity.ok(result);
    }
}
