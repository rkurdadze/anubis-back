package ge.comcom.anubis.controller.meta;

import ge.comcom.anubis.dto.ClassPropertyDto;
import ge.comcom.anubis.dto.ClassPropertyRequest;
import ge.comcom.anubis.service.meta.ClassPropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/meta/class-properties")
@RequiredArgsConstructor
@Tag(name = "Metadata: Class-Property Bindings", description = "Defines which properties belong to which class")
public class ClassPropertyController {

    private final ClassPropertyService service;

    @Operation(summary = "Assign property to class")
    @PostMapping
    public ClassPropertyDto create(@Valid @RequestBody ClassPropertyRequest req) {
        return service.create(req);
    }

    @Operation(summary = "List all properties of a class")
    @GetMapping("/by-class/{classId}")
    public List<ClassPropertyDto> listByClass(@PathVariable Long classId) {
        return service.listByClass(classId);
    }

    @Operation(summary = "Get binding by ID")
    @GetMapping("/{id}")
    public ClassPropertyDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @Operation(summary = "Update binding")
    @PutMapping("/{id}")
    public ClassPropertyDto update(@PathVariable Long id, @Valid @RequestBody ClassPropertyRequest req) {
        return service.update(id, req);
    }

    @Operation(summary = "Delete binding")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}

