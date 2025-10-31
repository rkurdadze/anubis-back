package ge.comcom.anubis.controller.core;

import ge.comcom.anubis.dto.FileStorageDto;
import ge.comcom.anubis.dto.FileStorageRequest;
import ge.comcom.anubis.entity.core.FileStorageEntity;
import ge.comcom.anubis.mapper.FileStorageMapper;
import ge.comcom.anubis.service.storage.FileStorageAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST-контроллер для управления конфигурациями хранилищ файлов.
 */
@RestController
@RequestMapping("/api/v1/file-storages")
@RequiredArgsConstructor
@Tag(name = "File storages", description = "Управление конфигурациями DB/FS/S3 хранилищ")
public class FileStorageController {

    private final FileStorageAdminService fileStorageAdminService;
    private final FileStorageMapper fileStorageMapper;

    @GetMapping
    @Operation(summary = "Список доступных конфигураций хранилищ")
    public List<FileStorageDto> getAll() {
        return fileStorageAdminService.findAll().stream()
                .map(fileStorageMapper::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить конфигурацию хранилища")
    public FileStorageDto getById(@PathVariable Long id) {
        return fileStorageMapper.toDto(fileStorageAdminService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Создать конфигурацию хранилища")
    public ResponseEntity<FileStorageDto> create(@Valid @RequestBody FileStorageRequest request) {
        var created = fileStorageAdminService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(fileStorageMapper.toDto(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить конфигурацию хранилища")
    public FileStorageDto update(@PathVariable Long id, @Valid @RequestBody FileStorageRequest request) {
        FileStorageEntity entity = fileStorageAdminService.update(id, request);
        FileStorageDto dto = fileStorageMapper.toDto(entity);
        return dto;
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить конфигурацию хранилища")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        fileStorageAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
