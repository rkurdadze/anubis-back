package ge.comcom.anubis.controller.meta;

import ge.comcom.anubis.dto.ClassPropertyDto;
import ge.comcom.anubis.dto.ClassPropertyRequest;
import ge.comcom.anubis.dto.EffectiveClassPropertyDto;
import ge.comcom.anubis.service.meta.ClassPropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @Operation(summary = "Получить эффективные свойства класса",
            description = "Возвращает свойства с учётом наследования и переопределений")
    @GetMapping("/by-class/{classId}/effective")
    public List<EffectiveClassPropertyDto> listEffectiveByClass(@PathVariable Long classId) {
        return service.listEffectiveByClass(classId);
    }

    @Operation(summary = "Get binding by composite key")
    @GetMapping("/{classId}/{propertyDefId}")
    public ClassPropertyDto get(@PathVariable Long classId, @PathVariable Long propertyDefId) {
        return service.get(classId, propertyDefId);
    }

    @Operation(summary = "Update binding")
    @PutMapping("/{classId}/{propertyDefId}")
    public ClassPropertyDto update(@PathVariable Long classId,
                                   @PathVariable Long propertyDefId,
                                   @Valid @RequestBody ClassPropertyRequest req) {
        return service.update(classId, propertyDefId, req);
    }

    @Operation(summary = "Delete binding")
    @DeleteMapping("/{classId}/{propertyDefId}")
    public void delete(@PathVariable Long classId, @PathVariable Long propertyDefId) {
        service.delete(classId, propertyDefId);
    }

    @PatchMapping("/{classId}/{propertyDefId}/deactivate")
    @Operation(summary = "Деактивировать свойство класса (soft-delete)")
    public ResponseEntity<Void> deactivate(@PathVariable Long classId, @PathVariable Long propertyDefId) {
        service.deactivate(classId, propertyDefId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{classId}/{propertyDefId}/activate")
    @Operation(summary = "Активировать свойство класса (reactivate after soft-delete)")
    public ResponseEntity<Void> activate(@PathVariable Long classId, @PathVariable Long propertyDefId) {
        service.activate(classId, propertyDefId);
        return ResponseEntity.noContent().build();
    }


}

