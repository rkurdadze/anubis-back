package ge.comcom.anubis.controller.core;

import ge.comcom.anubis.dto.ObjectDto;
import ge.comcom.anubis.dto.ObjectLinkDto;
import ge.comcom.anubis.dto.ObjectLinksDto;
import ge.comcom.anubis.entity.core.ObjectEntity;
import ge.comcom.anubis.enums.LinkDirection;
import ge.comcom.anubis.mapper.ObjectLinkMapper;
import ge.comcom.anubis.mapper.ObjectMapper;
import ge.comcom.anubis.service.core.ObjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST controller for managing logical repository objects.
 * <p>
 * Provides CRUD operations, soft/hard deletion, and relationship (link) management
 * between repository objects, following M-Files-like model.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/objects")
@RequiredArgsConstructor
@Tag(name = "Objects", description = "API for managing logical repository objects and relationships")
public class ObjectController {

    private final ObjectService objectService;
    private final ObjectMapper mapper;
    private final ObjectLinkMapper linkMapper;

    private static final Logger log = LoggerFactory.getLogger(ObjectController.class);

    // ============================================================
    // CRUD OPERATIONS
    // ============================================================

    /**
     * Retrieves all (optionally filtered) repository objects with pagination.
     *
     * @param pageable pagination and sorting information
     * @param filters filter parameters as map
     * @return page of objects as DTOs
     */
    @GetMapping
    @Operation(summary = "Get all (optionally filtered) objects",
            description = "Returns paginated objects filtered by type, class, search string and deletion flag.")
    @ApiResponse(responseCode = "200", description = "Page of objects returned successfully.")
    public Page<ObjectDto> getAll(Pageable pageable, @RequestParam Map<String, String> filters) {
        Long typeId = parseLongSafe(filters.get("typeId"));
        Long classId = parseLongSafe(filters.get("classId"));
        String search = filters.getOrDefault("search", null);
        boolean showDeleted = Boolean.parseBoolean(filters.getOrDefault("showDeleted", "false"));

        log.info("📦 Filtering objects: typeId={}, classId={}, search='{}', showDeleted={}",
                typeId, classId, search, showDeleted);

        return objectService.getAllFiltered(pageable, typeId, classId, search, showDeleted)
                .map(mapper::toDto);
    }

    private Long parseLongSafe(String value) {
        try {
            return (value == null || value.isBlank()) ? null : Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Retrieves a specific object by its ID.
     *
     * @param id unique identifier of the object
     * @return object DTO representation
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get object by ID",
            description = "Returns a single repository object entity by its database ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Object retrieved successfully."),
            @ApiResponse(responseCode = "404", description = "Object not found.")
    })
    public ObjectDto getById(
            @Parameter(description = "Object ID", example = "1001") @PathVariable Long id) {
        return mapper.toDto(objectService.getById(id));
    }

    /**
     * Creates a new repository object.
     *
     * @param dto DTO representation of the new object
     * @return created object DTO
     */
    @PostMapping
    @Operation(summary = "Create new object",
            description = "Creates a new logical repository object and persists it.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Object created successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid request payload.")
    })
    public ObjectDto create(@Valid @RequestBody ObjectDto dto) {
        ObjectEntity saved = objectService.create(dto);
        return mapper.toDto(saved);
    }

    /**
     * Updates an existing repository object.
     *
     * @param id  object ID to update
     * @param dto new data for the object
     * @return updated object DTO
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update object",
            description = "Updates an existing logical repository object by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Object updated successfully."),
            @ApiResponse(responseCode = "404", description = "Object not found.")
    })
    public ObjectDto update(
            @Parameter(description = "Object ID", example = "1001") @PathVariable Long id,
            @Valid @RequestBody ObjectDto dto) {
        ObjectEntity updated = objectService.update(id, dto);
        return mapper.toDto(updated);
    }

    /**
     * Performs a soft deletion (logical delete) of an object.
     * The record remains in the database but marked as deleted.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete object",
            description = "Marks an object as deleted without removing it physically.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Object marked as deleted."),
            @ApiResponse(responseCode = "404", description = "Object not found.")
    })
    public ResponseEntity<Void> softDelete(
            @Parameter(description = "Object ID", example = "1001") @PathVariable Long id) {
        objectService.softDelete(id, null); // TODO: pass authenticated user later
        return ResponseEntity.noContent().build();
    }

    /**
     * Performs a physical (hard) deletion of an object from the database.
     */
    @DeleteMapping("/{id}/hard")
    @Operation(summary = "Hard delete object",
            description = "Permanently removes an object from the database.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Object deleted permanently."),
            @ApiResponse(responseCode = "404", description = "Object not found.")
    })
    public ResponseEntity<Void> hardDelete(
            @Parameter(description = "Object ID", example = "1001") @PathVariable Long id) {
        objectService.hardDelete(id);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // RELATIONSHIPS (LINKS)
    // ============================================================

    /**
     * Retrieves an object along with all its related links.
     * Returns both incoming and outgoing object relationships.
     */
    @GetMapping("/{id}/links")
    @Operation(summary = "Get object with links",
            description = "Returns the object along with all related incoming and outgoing links.")
    @ApiResponse(responseCode = "200", description = "Object with its links returned successfully.")
    public ResponseEntity<ObjectLinksDto> getObjectWithLinks(
            @Parameter(description = "Object ID", example = "1001") @PathVariable Long id) {
        ObjectEntity entity = objectService.getWithLinks(id);
        ObjectLinksDto response = ObjectLinksDto.builder()
                .object(mapper.toDto(entity))
                .outgoing(linkMapper.toDto(entity.getOutgoingLinks()))
                .incoming(linkMapper.toDto(entity.getIncomingLinks()))
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all outgoing links (object → others) for a specific object.
     */
    @GetMapping("/{id}/links/outgoing")
    @Operation(summary = "Get outgoing links",
            description = "Returns all outgoing relationships (object → others) for the given object.")
    @ApiResponse(responseCode = "200", description = "Outgoing links returned successfully.")
    public ResponseEntity<List<ObjectLinkDto>> getOutgoing(
            @Parameter(description = "Object ID", example = "1001") @PathVariable Long id) {
        return ResponseEntity.ok(linkMapper.toDto(objectService.getOutgoingLinks(id)));
    }

    /**
     * Retrieves all incoming links (others → object) for a specific object.
     */
    @GetMapping("/{id}/links/incoming")
    @Operation(summary = "Get incoming links",
            description = "Returns all incoming relationships (others → object) for the given object.")
    @ApiResponse(responseCode = "200", description = "Incoming links returned successfully.")
    public ResponseEntity<List<ObjectLinkDto>> getIncoming(
            @Parameter(description = "Object ID", example = "1001") @PathVariable Long id) {
        return ResponseEntity.ok(linkMapper.toDto(objectService.getIncomingLinks(id)));
    }

    /**
     * Creates a new link (relationship) between two objects.
     * <p>
     * If the link direction is BI (bidirectional), a reciprocal link will be automatically created.
     * </p>
     *
     * Example:
     * <pre>
     * POST /api/v1/objects/link?sourceId=1&targetId=2&role=Customer&direction=BI
     * </pre>
     */
    @PostMapping("/link")
    @Operation(summary = "Create object link",
            description = "Creates a new relationship between two objects. "
                    + "If direction is BI (bidirectional), the reverse link is created automatically.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Link(s) created successfully."),
            @ApiResponse(responseCode = "404", description = "One or both target objects not found.")
    })
    public ResponseEntity<ObjectLinkDto> createLink(
            @Parameter(description = "Source object ID", example = "1") @RequestParam Long sourceId,
            @Parameter(description = "Target object ID", example = "2") @RequestParam Long targetId,
            @Parameter(description = "Relationship role name", example = "Customer") @RequestParam String role,
            @Parameter(description = "Link direction (UNI or BI)", example = "BI")
            @RequestParam(defaultValue = "UNI") LinkDirection direction
    ) {
        return ResponseEntity.ok(linkMapper.toDto(objectService.createLink(sourceId, targetId, role, direction)));
    }

    /**
     * Deletes all links between two objects for the specified relationship role.
     * <p>
     * For bidirectional links (BI), both directions are removed.
     * </p>
     *
     * Example:
     * <pre>
     * DELETE /api/v1/objects/link?sourceId=1&targetId=2&role=Customer
     * </pre>
     */
    @DeleteMapping("/link")
    @Operation(summary = "Delete object link",
            description = "Removes relationship(s) between two objects for the specified role. "
                    + "For bidirectional links, both directions are deleted.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Link(s) deleted successfully."),
            @ApiResponse(responseCode = "404", description = "No matching link found.")
    })
    public ResponseEntity<Void> deleteLink(
            @Parameter(description = "Source object ID", example = "1") @RequestParam Long sourceId,
            @Parameter(description = "Target object ID", example = "2") @RequestParam Long targetId,
            @Parameter(description = "Relationship role name", example = "Customer") @RequestParam String role
    ) {
        objectService.removeLink(sourceId, targetId, role);
        return ResponseEntity.noContent().build();
    }
}
