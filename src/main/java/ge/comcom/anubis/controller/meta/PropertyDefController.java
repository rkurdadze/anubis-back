package ge.comcom.anubis.controller.meta;

import ge.comcom.anubis.dto.meta.PropertyDefDto;
import ge.comcom.anubis.dto.meta.PropertyDefRequest;
import ge.comcom.anubis.service.meta.PropertyDefService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/meta/property-defs")
@RequiredArgsConstructor
@Tag(name = "Metadata: Property Definitions", description = "CRUD API for metadata property definitions")
public class PropertyDefController {

    private final PropertyDefService service;

    @Operation(summary = "Create new property definition")
    @PostMapping
    public PropertyDefDto create(@Valid @RequestBody PropertyDefRequest req) {
        return service.create(req);
    }

    @Operation(summary = "List all property definitions")
    @GetMapping
    public Page<PropertyDefDto> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 200));
        return service.list(pageable);
    }

    @Operation(summary = "Get property definition by ID")
    @GetMapping("/{id}")
    public PropertyDefDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @Operation(summary = "Update property definition")
    @PutMapping("/{id}")
    public PropertyDefDto update(@PathVariable Long id, @Valid @RequestBody PropertyDefRequest req) {
        return service.update(id, req);
    }

    @Operation(summary = "Delete property definition")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}

