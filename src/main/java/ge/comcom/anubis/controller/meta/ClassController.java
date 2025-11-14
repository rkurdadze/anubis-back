package ge.comcom.anubis.controller.meta;

import ge.comcom.anubis.dto.ClassDto;
import ge.comcom.anubis.dto.ClassRequest;
import ge.comcom.anubis.dto.ClassTreeNodeDto;
import ge.comcom.anubis.service.meta.ClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for metadata class management.
 */
@RestController
@RequestMapping("/api/v1/meta/classes")
@RequiredArgsConstructor
@Tag(name = "Metadata: Classes", description = "CRUD API for managing metadata class definitions")
public class ClassController {

    private final ClassService classService;

    @Operation(summary = "Create a new class definition",
            description = "Creates a new metadata class linked to ObjectType and optionally ACL.")
    @ApiResponse(responseCode = "200", description = "Class successfully created")
    @PostMapping
    public ClassDto create(@Valid @RequestBody ClassRequest request) {
        return classService.create(request);
    }

    @Operation(summary = "List all class definitions", description = "Returns paginated list of metadata classes.")
    @GetMapping
    public Page<ClassDto> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,asc") String sort) {

        String[] parts = sort.split(",", 2);
        Sort.Direction dir = parts.length > 1 ? Sort.Direction.fromString(parts[1]) : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, Math.min(size, 200), Sort.by(dir, parts[0]));
        return classService.list(pageable);
    }

    @Operation(summary = "Получить иерархию классов",
            description = "Возвращает дерево классов с учётом наследования. Можно фильтровать по ObjectType")
    @GetMapping("/tree")
    public List<ClassTreeNodeDto> tree(@RequestParam(value = "objectTypeId", required = false) Long objectTypeId) {
        return classService.getHierarchy(objectTypeId);
    }

    @Operation(summary = "Get class by ID", description = "Fetches a single metadata class by its ID.")
    @GetMapping("/{id}")
    public ClassDto get(@PathVariable Long id) {
        return classService.get(id);
    }

    @Operation(summary = "Update existing class", description = "Updates name, description, ACL or active status of a class.")
    @PutMapping("/{id}")
    public ClassDto update(@PathVariable Long id, @Valid @RequestBody ClassRequest request) {
        return classService.update(id, request);
    }

    @Operation(summary = "Delete class", description = "Deletes a metadata class definition by its ID.")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        classService.delete(id);
    }
}
