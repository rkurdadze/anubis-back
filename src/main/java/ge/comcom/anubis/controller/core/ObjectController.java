package ge.comcom.anubis.controller.core;

import ge.comcom.anubis.dto.core.ObjectDto;
import ge.comcom.anubis.dto.mapper.ObjectMapper;
import ge.comcom.anubis.entity.core.ObjectEntity;
import ge.comcom.anubis.service.core.ObjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing base repository objects (documents, projects, customers, etc.).
 *
 * Objects are core entities of the system that can have versions, files, workflows, and properties.
 */
@RestController
@RequestMapping("/api/v1/objects")
@RequiredArgsConstructor
@Tag(
        name = "Objects",
        description = "CRUD operations for repository objects (like M-Files base entities)."
)
public class ObjectController {

    private final ObjectService objectService;
    private final ObjectMapper objectMapper;

    // ==========================================================
    // GET all objects
    // ==========================================================
    @Operation(
            summary = "Get all objects",
            description = "Returns a list of all repository objects, including their type and class references.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of objects retrieved successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ObjectDto.class)))
            }
    )
    @GetMapping
    public List<ObjectDto> getAll() {
        return objectService.findAll()
                .stream()
                .map(objectMapper::toDto)
                .toList();
    }

    // ==========================================================
    // GET by ID
    // ==========================================================
    @Operation(
            summary = "Get object by ID",
            description = "Fetches a single repository object by its unique identifier.",
            parameters = {
                    @Parameter(name = "id", description = "Unique identifier of the object", example = "1")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Object found",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ObjectDto.class))),
                    @ApiResponse(responseCode = "404", description = "Object not found")
            }
    )
    @GetMapping("/{id}")
    public ResponseEntity<ObjectDto> getById(@PathVariable Long id) {
        return objectService.findById(id)
                .map(objectMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ==========================================================
    // POST create
    // ==========================================================
    @Operation(
            summary = "Create new object",
            description = "Creates a new repository object (for example, a document or a project).",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "New object definition",
                    content = @Content(schema = @Schema(implementation = ObjectDto.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Object created successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ObjectDto.class)))
            }
    )
    @PostMapping
    public ResponseEntity<ObjectDto> create(@RequestBody ObjectDto dto) {
        ObjectEntity entity = objectMapper.toEntity(dto);
        ObjectDto saved = objectMapper.toDto(objectService.save(entity));
        return ResponseEntity.status(201).body(saved);
    }

    // ==========================================================
    // PUT update
    // ==========================================================
    @Operation(
            summary = "Update existing object",
            description = "Updates an existing repository object by ID.",
            parameters = {
                    @Parameter(name = "id", description = "Unique ID of the object to update", example = "1")
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Updated object definition",
                    content = @Content(schema = @Schema(implementation = ObjectDto.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Object updated successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ObjectDto.class))),
                    @ApiResponse(responseCode = "404", description = "Object not found")
            }
    )
    @PutMapping("/{id}")
    public ResponseEntity<ObjectDto> update(@PathVariable Long id, @RequestBody ObjectDto dto) {
        return objectService.findById(id)
                .map(existing -> {
                    existing.setName(dto.getName());
                    ObjectDto updated = objectMapper.toDto(objectService.save(existing));
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ==========================================================
    // DELETE by ID
    // ==========================================================
    @Operation(
            summary = "Delete object",
            description = "Deletes an object and all its associated versions and files.",
            parameters = {
                    @Parameter(name = "id", description = "Unique ID of the object", example = "1")
            },
            responses = {
                    @ApiResponse(responseCode = "204", description = "Object deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Object not found")
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        objectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
