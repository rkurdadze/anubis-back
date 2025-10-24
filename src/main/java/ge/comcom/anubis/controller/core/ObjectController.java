package ge.comcom.anubis.controller.core;

import ge.comcom.anubis.dto.ObjectDto;
import ge.comcom.anubis.mapper.ObjectMapper;
import ge.comcom.anubis.entity.core.ObjectEntity;
import ge.comcom.anubis.service.core.ObjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing repository objects.
 */
@RestController
@RequestMapping("/api/objects")
@RequiredArgsConstructor
@Tag(name = "Objects", description = "API for managing core repository objects")
public class ObjectController {

    private final ObjectService objectService;
    private final ObjectMapper mapper;

    @GetMapping
    @Operation(summary = "Get all active objects", description = "Returns all active (non-archived) objects")
    public List<ObjectDto> getAll() {
        return objectService.getAllActive().stream()
                .map(mapper::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get object by ID", description = "Returns a single object entity by its ID")
    public ObjectDto getById(
            @Parameter(description = "Object ID", example = "1001") @PathVariable Long id) {
        return mapper.toDto(objectService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create new object", description = "Creates a new repository object")
    public ObjectDto create(@RequestBody ObjectDto dto) {
        ObjectEntity entity = mapper.toEntity(dto);
        ObjectEntity saved = objectService.create(entity);
        return mapper.toDto(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update object", description = "Updates the existing object entity by ID")
    public ObjectDto update(
            @Parameter(description = "Object ID", example = "1001") @PathVariable Long id,
            @RequestBody ObjectDto dto) {
        ObjectEntity entity = mapper.toEntity(dto);
        ObjectEntity updated = objectService.update(id, entity);
        return mapper.toDto(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete object", description = "Deletes an object entity by ID")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Object ID", example = "1001") @PathVariable Long id) {
        objectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
