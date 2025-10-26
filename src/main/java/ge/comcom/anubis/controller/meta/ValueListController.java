package ge.comcom.anubis.controller.meta;

import ge.comcom.anubis.dto.ValueListDto;
import ge.comcom.anubis.service.meta.ValueListService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/meta/value-lists")
@RequiredArgsConstructor
@Tag(name = "Metadata: Value Lists", description = "CRUD API for metadata dictionaries (value lists)")
public class ValueListController {

    private final ValueListService service;

    @Operation(summary = "Create a new value list")
    @PostMapping
    public ValueListDto create(@Valid @RequestBody ValueListDto req) {
        return service.create(req);
    }

    @Operation(summary = "List value lists")
    @GetMapping
    public Page<ValueListDto> list(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 200));
        return service.list(pageable);
    }

    @Operation(summary = "Get value list by ID")
    @GetMapping("/{id}")
    public ValueListDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @Operation(summary = "Update value list")
    @PutMapping("/{id}")
    public ValueListDto update(@PathVariable Long id, @Valid @RequestBody ValueListDto req) {
        return service.update(id, req);
    }

    @Operation(summary = "Delete value list")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }


    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Деактивировать ValueList (soft-delete)")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }

}

