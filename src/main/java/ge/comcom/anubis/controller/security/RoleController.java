package ge.comcom.anubis.controller.security;

import ge.comcom.anubis.dto.RoleDto;
import ge.comcom.anubis.dto.RoleRequest;
import ge.comcom.anubis.service.security.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/security/roles")
@RequiredArgsConstructor
@Tag(name = "Security - Roles", description = "Управление ролевой моделью (Named Roles)")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @Operation(summary = "Список ролей")
    public List<RoleDto> list() {
        return roleService.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить роль по идентификатору")
    public RoleDto get(@PathVariable Long id) {
        return roleService.get(id);
    }

    @PostMapping
    @Operation(summary = "Создать новую роль")
    public RoleDto create(@Valid @RequestBody RoleRequest request) {
        return roleService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить роль")
    public RoleDto update(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        return roleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить роль")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
