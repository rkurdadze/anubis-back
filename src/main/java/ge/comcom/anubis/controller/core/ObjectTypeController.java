package ge.comcom.anubis.controller.core;

import ge.comcom.anubis.dto.ObjectTypeDto;
import ge.comcom.anubis.mapper.ObjectTypeMapper;
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
    private final ObjectTypeMapper mapper;

    @GetMapping
    public List<ObjectTypeDto> getAll() {
        return objectTypeService.findAll()
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ObjectTypeDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toDto(objectTypeService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ObjectTypeDto> create(@RequestBody ObjectTypeDto dto) {
        return ResponseEntity.ok(mapper.toDto(objectTypeService.create(mapper.toEntity(dto))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ObjectTypeDto> update(@PathVariable Long id, @RequestBody ObjectTypeDto dto) {
        return ResponseEntity.ok(mapper.toDto(objectTypeService.update(id, mapper.toEntity(dto))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        objectTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
