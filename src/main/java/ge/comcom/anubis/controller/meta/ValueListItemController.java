package ge.comcom.anubis.controller.meta;

import ge.comcom.anubis.dto.ValueListItemDto;
import ge.comcom.anubis.service.meta.ValueListItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/meta/value-list-items")
@RequiredArgsConstructor
@Tag(name = "Metadata: Value List Items", description = "CRUD API for individual items in value lists")
public class ValueListItemController {

    private final ValueListItemService service;

    @Operation(summary = "Create a new item in a value list")
    @PostMapping
    public ValueListItemDto create(@Valid @RequestBody ValueListItemDto req) {
        return service.create(req);
    }

    @Operation(summary = "List all items in a value list")
    @GetMapping("/by-list/{listId}")
    public List<ValueListItemDto> listByList(@PathVariable("listId") Long listId) {
        return service.listByListId(listId);
    }

    @Operation(summary = "Get item by ID")
    @GetMapping("/{id}")
    public ValueListItemDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @Operation(summary = "Update item")
    @PutMapping("/{id}")
    public ValueListItemDto update(@PathVariable Long id, @Valid @RequestBody ValueListItemDto req) {
        return service.update(id, req);
    }

    @Operation(summary = "Delete item")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }


    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Деактивировать элемент справочника (soft-delete)")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }

}

