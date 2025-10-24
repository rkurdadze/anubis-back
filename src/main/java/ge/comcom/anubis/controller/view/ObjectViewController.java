package ge.comcom.anubis.controller.view;


import ge.comcom.anubis.dto.view.ObjectViewDto;
import ge.comcom.anubis.entity.view.ObjectViewEntity;
import ge.comcom.anubis.service.view.ObjectViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing saved views.
 */
@RestController
@RequestMapping("/api/v1/views")
@RequiredArgsConstructor
public class ObjectViewController {

    private final ObjectViewService service;

    @PostMapping
    public ResponseEntity<ObjectViewEntity> create(@RequestBody ObjectViewDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ObjectViewEntity> update(@PathVariable Long id, @RequestBody ObjectViewDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/available/{userId}")
    public ResponseEntity<List<ObjectViewEntity>> getAvailable(@PathVariable Long userId) {
        return ResponseEntity.ok(service.getAvailable(userId));
    }
}

