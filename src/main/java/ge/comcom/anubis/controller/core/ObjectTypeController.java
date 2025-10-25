package ge.comcom.anubis.controller.core;

import ge.comcom.anubis.entity.core.ObjectType;
import ge.comcom.anubis.service.core.ObjectTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/object-types")
@RequiredArgsConstructor
public class ObjectTypeController {

    private final ObjectTypeService objectTypeService;

    @GetMapping
    public List<ObjectType> getAll() {
        return objectTypeService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ObjectType> getById(@PathVariable Long id) {
        return ResponseEntity.ok(objectTypeService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ObjectType> create(@RequestBody ObjectType objectType) {
        return ResponseEntity.ok(objectTypeService.create(objectType));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ObjectType> update(@PathVariable Long id, @RequestBody ObjectType objectType) {
        return ResponseEntity.ok(objectTypeService.update(id, objectType));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        objectTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
