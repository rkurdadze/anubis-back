package ge.comcom.anubis.controller.core;

import ge.comcom.anubis.entity.core.ObjectLinkEntity;
import ge.comcom.anubis.enums.LinkDirection;
import ge.comcom.anubis.service.core.ObjectLinkService;
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
 * REST controller for managing object-to-object relationships.
 * <p>
 * Provides endpoints for creating, deleting, and retrieving logical links between repository objects.
 * Works similarly to M-Files "Relationships" feature (e.g., Document → Customer).
 * </p>
 */
@RestController
@RequestMapping("/api/v1/links")
@RequiredArgsConstructor
@Tag(name = "Object Links", description = "API for managing relationships (links) between repository objects")
public class ObjectLinkController {

    private final ObjectLinkService linkService;

    // ============================================================
    // CREATE LINK
    // ============================================================

    /**
     * Creates a new link between two objects.
     * <p>
     * If the link direction is set to {@code BI}, a reciprocal (reverse) link is automatically created.
     * </p>
     *
     * Example:
     * <pre>
     * POST /api/v1/links?srcId=1&dstId=2&role=Customer&direction=BI
     * </pre>
     *
     * @param srcId      ID of the source object
     * @param dstId      ID of the destination object
     * @param role       Role name describing the relationship (must exist in link_role table)
     * @param direction  Link direction (UNI = one-way, BI = bidirectional)
     * @return created {@link ObjectLinkEntity}
     */
    @PostMapping
    @Operation(
            summary = "Create link between objects",
            description = "Creates a new relationship between two objects. "
                    + "If the direction is BI, a reverse link is automatically added."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Link created successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid link parameters."),
            @ApiResponse(responseCode = "404", description = "One or both objects not found.")
    })
    public ResponseEntity<ObjectLinkEntity> createLink(
            @Parameter(description = "Source object ID", example = "1")
            @RequestParam Long srcId,
            @Parameter(description = "Destination object ID", example = "2")
            @RequestParam Long dstId,
            @Parameter(description = "Relationship role (must exist in link_role table)", example = "Customer")
            @RequestParam String role,
            @Parameter(description = "Direction: UNI (one-way) or BI (bidirectional)", example = "BI")
            @RequestParam(defaultValue = "UNI") LinkDirection direction
    ) {
        ObjectLinkEntity link = linkService.createLink(srcId, dstId, role, direction);
        return ResponseEntity.ok(link);
    }

    // ============================================================
    // DELETE LINK
    // ============================================================

    /**
     * Deletes existing link(s) between two objects for the given role.
     * <p>
     * If the link was bidirectional, both directions will be removed automatically.
     * </p>
     *
     * Example:
     * <pre>
     * DELETE /api/v1/links?srcId=1&dstId=2&role=Customer
     * </pre>
     *
     * @param srcId  ID of the source object
     * @param dstId  ID of the destination object
     * @param role   Role name of the relationship
     */
    @DeleteMapping
    @Operation(
            summary = "Delete link between objects",
            description = "Deletes the link(s) between two objects for the specified role. "
                    + "If the link is bidirectional, both directions will be deleted."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Link deleted successfully."),
            @ApiResponse(responseCode = "404", description = "Link not found.")
    })
    public ResponseEntity<Void> deleteLink(
            @Parameter(description = "Source object ID", example = "1") @RequestParam Long srcId,
            @Parameter(description = "Destination object ID", example = "2") @RequestParam Long dstId,
            @Parameter(description = "Relationship role", example = "Customer") @RequestParam String role
    ) {
        linkService.removeLink(srcId, dstId, role);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // GET LINKS
    // ============================================================

    /**
     * Retrieves all links (outgoing and incoming) for a given object.
     * <p>
     * The response includes both relationships where the given object
     * is the source or the destination, including role and direction.
     * </p>
     *
     * Example:
     * <pre>
     * GET /api/v1/links/5
     * </pre>
     *
     * @param objectId ID of the object whose links should be retrieved
     * @return list of {@link ObjectLinkEntity} representing all relationships
     */
    @GetMapping("/{objectId}")
    @Operation(
            summary = "Get object links",
            description = "Returns all relationships (incoming and outgoing) for the specified object."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Links retrieved successfully."),
            @ApiResponse(responseCode = "404", description = "Object not found.")
    })
    public ResponseEntity<List<ObjectLinkEntity>> getLinks(
            @Parameter(description = "Object ID whose links to retrieve", example = "5")
            @PathVariable Long objectId
    ) {
        return ResponseEntity.ok(linkService.getLinks(objectId));
    }
}
