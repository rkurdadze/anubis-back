package ge.comcom.anubis.controller.security;

import ge.comcom.anubis.dto.AclDto;
import ge.comcom.anubis.dto.AclEntryDto;
import ge.comcom.anubis.dto.AclEntryRequest;
import ge.comcom.anubis.dto.AclRequest;
import ge.comcom.anubis.service.security.AclManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/security/acls")
@RequiredArgsConstructor
@Tag(name = "Security - ACL", description = "Управление правами доступа")
public class AclController {

    private final AclManagementService aclManagementService;

    @GetMapping
    @Operation(summary = "Список ACL с записями")
    public List<AclDto> list() {
        return aclManagementService.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить ACL по идентификатору")
    public AclDto get(@PathVariable Long id) {
        return aclManagementService.get(id);
    }

    @PostMapping
    @Operation(summary = "Создать новый ACL")
    public AclDto create(@Valid @RequestBody AclRequest request) {
        return aclManagementService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить ACL")
    public AclDto update(@PathVariable Long id, @Valid @RequestBody AclRequest request) {
        return aclManagementService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить ACL")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        aclManagementService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{aclId}/entries")
    @Operation(summary = "Список записей ACL")
    public List<AclEntryDto> listEntries(@PathVariable Long aclId) {
        return aclManagementService.listEntries(aclId);
    }

    @GetMapping("/{aclId}/entries/{entryId}")
    @Operation(summary = "Получить запись ACL")
    public AclEntryDto getEntry(@PathVariable Long aclId, @PathVariable Long entryId) {
        return aclManagementService.getEntry(aclId, entryId);
    }

    @PostMapping("/{aclId}/entries")
    @Operation(summary = "Создать запись ACL")
    public AclEntryDto createEntry(@PathVariable Long aclId, @Valid @RequestBody AclEntryRequest request) {
        return aclManagementService.createEntry(aclId, request);
    }

    @PutMapping("/{aclId}/entries/{entryId}")
    @Operation(summary = "Обновить запись ACL")
    public AclEntryDto updateEntry(@PathVariable Long aclId, @PathVariable Long entryId,
                                   @Valid @RequestBody AclEntryRequest request) {
        return aclManagementService.updateEntry(aclId, entryId, request);
    }

    @DeleteMapping("/{aclId}/entries/{entryId}")
    @Operation(summary = "Удалить запись ACL")
    public ResponseEntity<Void> deleteEntry(@PathVariable Long aclId, @PathVariable Long entryId) {
        aclManagementService.deleteEntry(aclId, entryId);
        return ResponseEntity.noContent().build();
    }
}
