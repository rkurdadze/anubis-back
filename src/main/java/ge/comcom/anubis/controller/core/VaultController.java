package ge.comcom.anubis.controller.core;

import ge.comcom.anubis.dto.VaultDto;
import ge.comcom.anubis.dto.VaultRequest;
import ge.comcom.anubis.service.storage.VaultAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST-контроллер для управления логическими vault'ами.
 */
@RestController
@RequestMapping("/api/v1/vaults")
@RequiredArgsConstructor
@Tag(name = "Vaults", description = "Управление логическими хранилищами и их настройками")
public class VaultController {

    private final VaultAdminService vaultAdminService;
    @GetMapping
    @Operation(summary = "Список всех vault'ов")
    public List<VaultDto> getAll() {
        return vaultAdminService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить vault по идентификатору")
    public VaultDto getById(@PathVariable Long id) {
        return vaultAdminService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Создать новый vault")
    public ResponseEntity<VaultDto> create(@Valid @RequestBody VaultRequest request) {
        var created = vaultAdminService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить существующий vault")
    public VaultDto update(@PathVariable Long id, @Valid @RequestBody VaultRequest request) {
        return vaultAdminService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить vault")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        vaultAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
