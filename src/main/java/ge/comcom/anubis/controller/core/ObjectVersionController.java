package ge.comcom.anubis.controller.core;

import ge.comcom.anubis.entity.core.ObjectVersion;
import ge.comcom.anubis.service.core.ObjectVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/versions")
@RequiredArgsConstructor
public class ObjectVersionController {

    private final ObjectVersionService objectVersionService;

    @GetMapping("/object/{objectId}")
    public List<ObjectVersion> getVersions(@PathVariable Long objectId) {
        return objectVersionService.getVersions(objectId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ObjectVersion> getById(@PathVariable Long id) {
        return objectVersionService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ObjectVersion create(@RequestBody ObjectVersion version) {
        return objectVersionService.save(version);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        objectVersionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
