package ge.comcom.anubis.controller.view;

import ge.comcom.anubis.dto.view.ObjectViewDto;
import ge.comcom.anubis.entity.core.ObjectEntity;
import ge.comcom.anubis.entity.view.ObjectViewEntity;
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

    // ============================================================
    // CRUD OPERATIONS
    // ============================================================

    /**
     * Creates a new saved view definition.
     *
     * @param dto DTO representing the view to create
     * @return created view entity
     */
    @PostMapping
    @Operation(summary = "Create new view",
            description = "Creates a new saved view definition (virtual folder or search preset).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "View created successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid view definition payload.")
    })
    public ResponseEntity<ObjectViewEntity> create(
            @Parameter(description = "View DTO containing filter and grouping configuration")
            @RequestBody ObjectViewDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    /**
     * Updates an existing saved view by ID.
     *
     * @param id  identifier of the view to update
     * @param dto updated view definition
     * @return updated view entity
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update view",
            description = "Updates an existing saved view definition by its ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "View updated successfully."),
            @ApiResponse(responseCode = "404", description = "View not found.")
    })
    public ResponseEntity<ObjectViewEntity> update(
            @Parameter(description = "View ID", example = "101") @PathVariable Long id,
            @Parameter(description = "Updated view definition") @RequestBody ObjectViewDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    /**
     * Deletes a saved view by its ID.
     *
     * @param id identifier of the view to delete
     */
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

    /**
     * Retrieves all available views for a specific user.
     * <p>
     * Includes both personal (non-shared) and common (shared) views.
     * </p>
     *
     * @param userId user identifier
     * @return list of accessible view definitions
     */
    @GetMapping("/available/{userId}")
    @Operation(summary = "Get available views for user",
            description = "Returns all personal and shared views accessible by the given user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of available views returned successfully."),
            @ApiResponse(responseCode = "404", description = "User not found or no available views.")
    })
    public ResponseEntity<List<ObjectViewEntity>> getAvailable(
            @Parameter(description = "User ID", example = "5") @PathVariable Long userId) {
        return ResponseEntity.ok(service.getAvailable(userId));
    }

    // ============================================================
    // EXECUTION
    // ============================================================

    /**
     * Executes a saved view and returns the matching repository objects.
     * <p>
     * The view filters can include both property-based and relationship-based criteria.
     * For example:
     * <pre>
     * [
     *   {"property_def_id": 50, "op": "=", "value": "Active"},
     *   {"link_role": "Customer", "linked_object_id": 42},
     *   {"reverse_link_role": "Customer", "reverse_linked_object_id": 42}
     * ]
     * </pre>
     * </p>
     *
     * @param id identifier of the view to execute
     * @return list of matching objects
     */
    @GetMapping("/{id}/execute")
    @Operation(summary = "Execute view",
            description = "Executes the specified view and returns all repository objects "
                    + "matching its filters (including relational filters).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "View executed successfully."),
            @ApiResponse(responseCode = "404", description = "View not found or no matching objects.")
    })
    public ResponseEntity<List<ObjectEntity>> executeView(
            @Parameter(description = "View ID", example = "101") @PathVariable Long id) {
        return ResponseEntity.ok(service.executeView(id));
    }
}
