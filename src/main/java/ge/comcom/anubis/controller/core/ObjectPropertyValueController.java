package ge.comcom.anubis.controller.core;

import ge.comcom.anubis.dto.PropertyValueDto;
import ge.comcom.anubis.service.core.ObjectPropertyValueService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST контроллер для управления свойствами ObjectVersion (аналог M-Files properties).
 */
@RestController
@RequestMapping("/api/v1/object-properties")
@RequiredArgsConstructor
public class ObjectPropertyValueController {

    private final ObjectPropertyValueService service;

    // --- Сохранить свойства ---
    @PostMapping("/{versionId}")
    @Operation(summary = "Сохранить все property значения для ObjectVersion")
    public ResponseEntity<Void> saveProperties(
            @PathVariable Long versionId,
            @RequestBody List<PropertyValueDto> properties
    ) {
        service.savePropertyValues(versionId, properties);
        return ResponseEntity.ok().build();
    }

    // --- Получить свойства ---
    @GetMapping("/{versionId}")
    @Operation(summary = "Получить все property значения для ObjectVersion")
    public ResponseEntity<List<PropertyValueDto>> getProperties(
            @PathVariable Long versionId
    ) {
        List<PropertyValueDto> values = service.getPropertyValues(versionId);
        return ResponseEntity.ok(values);
    }

    // --- Удалить все свойства ---
    @DeleteMapping("/{versionId}")
    @Operation(summary = "Удалить все property значения для ObjectVersion")
    public ResponseEntity<Void> deleteProperties(@PathVariable Long versionId) {
        service.deletePropertyValues(versionId);
        return ResponseEntity.noContent().build();
    }
}
