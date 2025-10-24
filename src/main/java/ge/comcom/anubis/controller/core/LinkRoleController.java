package ge.comcom.anubis.controller.core;

import ge.comcom.anubis.entity.core.LinkRole;
import ge.comcom.anubis.enums.LinkDirection;
import ge.comcom.anubis.repository.core.LinkRoleRepository;
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
 * REST controller for managing relationship roles (LinkRole).
 * <p>
 * These roles define the semantic meaning of relationships between objects,
 * such as "Customer", "Project", or "Attachment".
 * </p>
 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "Link Roles", description = "API for managing relationship roles used in object links")
public class LinkRoleController {

    private final LinkRoleRepository linkRoleRepository;

    // ============================================================
    // GET ALL ROLES
    // ============================================================

    /**
     * Returns all available link roles.
     *
     * Example:
     * <pre>
     * GET /api/v1/roles
     * </pre>
     */
    @GetMapping
    @Operation(summary = "List all link roles",
            description = "Returns all relationship roles (link_role) configured in the system.")
    @ApiResponse(responseCode = "200", description = "Roles retrieved successfully.")
    public ResponseEntity<List<LinkRole>> getAllRoles() {
        return ResponseEntity.ok(linkRoleRepository.findAll());
    }

    // ============================================================
    // GET BY ID
    // ============================================================

    /**
     * Returns a single link role by its ID.
     *
     * Example:
     * <pre>
     * GET /api/v1/roles/10
     * </pre>
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get role by ID",
            description = "Returns a specific link role (link_role) by its ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role found."),
            @ApiResponse(responseCode = "404", description = "Role not found.")
    })
    public ResponseEntity<LinkRole> getRoleById(
            @Parameter(description = "Role ID", example = "10") @PathVariable Long id
    ) {
        return linkRoleRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ============================================================
    // GET BY NAME
    // ============================================================

    /**
     * Returns a single link role by its name (case-insensitive).
     *
     * Example:
     * <pre>
     * GET /api/v1/roles/by-name/Customer
     * </pre>
     */
    @GetMapping("/by-name/{name}")
    @Operation(summary = "Get role by name",
            description = "Returns a specific link role (link_role) by its unique name.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role found."),
            @ApiResponse(responseCode = "404", description = "Role not found.")
    })
    public ResponseEntity<LinkRole> getRoleByName(
            @Parameter(description = "Role name (case-insensitive)", example = "Customer")
            @PathVariable String name
    ) {
        return linkRoleRepository.findByNameIgnoreCase(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ============================================================
    // CREATE ROLE
    // ============================================================

    /**
     * Creates a new link role.
     *
     * Example:
     * <pre>
     * POST /api/v1/roles
     * {
     *   "name": "Customer",
     *   "description": "Defines relation between document and customer",
     *   "direction": "UNI"
     * }
     * </pre>
     */
    @PostMapping
    @Operation(summary = "Create new role", description = "Creates a new relationship role (link_role).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role created successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid request body.")
    })
    public ResponseEntity<LinkRole> createRole(@RequestBody LinkRole role) {
        LinkRole saved = linkRoleRepository.save(role);
        return ResponseEntity.ok(saved);
    }

    // ============================================================
    // UPDATE ROLE
    // ============================================================

    /**
     * Updates an existing link role.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update existing role", description = "Updates role name, description, or direction.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role updated successfully."),
            @ApiResponse(responseCode = "404", description = "Role not found.")
    })
    public ResponseEntity<LinkRole> updateRole(
            @PathVariable Long id,
            @RequestBody LinkRole updatedRole
    ) {
        return linkRoleRepository.findById(id)
                .map(existing -> {
                    existing.setName(updatedRole.getName());
                    existing.setDescription(updatedRole.getDescription());
                    existing.setNameI18n(updatedRole.getNameI18n());
                    existing.setDirection(updatedRole.getDirection() != null ? updatedRole.getDirection() : LinkDirection.UNI);
                    existing.setIsActive(updatedRole.getIsActive());
                    LinkRole saved = linkRoleRepository.save(existing);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ============================================================
    // DELETE ROLE
    // ============================================================

    /**
     * Deletes a link role by ID.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete role", description = "Deletes a role by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Role deleted successfully."),
            @ApiResponse(responseCode = "404", description = "Role not found.")
    })
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        if (!linkRoleRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        linkRoleRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
