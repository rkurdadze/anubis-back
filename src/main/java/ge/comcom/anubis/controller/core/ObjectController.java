package ge.comcom.anubis.controller.core;

import ge.comcom.anubis.entity.core.ObjectEntity;
import ge.comcom.anubis.service.core.ObjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/objects")
@RequiredArgsConstructor
public class ObjectController {

    private final ObjectService objectService;

    @GetMapping
    public List<ObjectEntity> getAll() {
        return objectService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ObjectEntity> getById(@PathVariable Long id) {
        return objectService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ObjectEntity create(@RequestBody ObjectEntity objectEntity) {
        return objectService.save(objectEntity);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ObjectEntity> update(@PathVariable Long id, @RequestBody ObjectEntity updated) {
        return objectService.findById(id)
                .map(existing -> {
                    existing.setName(updated.getName());
                    existing.setObjectType(updated.getObjectType());
                    existing.setObjectClass(updated.getObjectClass());
                    return ResponseEntity.ok(objectService.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        objectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
