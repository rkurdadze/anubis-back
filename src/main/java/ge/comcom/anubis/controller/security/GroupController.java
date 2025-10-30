package ge.comcom.anubis.controller.security;

import ge.comcom.anubis.dto.GroupDto;
import ge.comcom.anubis.dto.GroupRequest;
import ge.comcom.anubis.service.security.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/security/groups")
@RequiredArgsConstructor
@Tag(name = "Security - Groups", description = "Управление ролями (группами)")
public class GroupController {

    private final GroupService groupService;

    @GetMapping
    @Operation(summary = "Список групп")
    public List<GroupDto> list() {
        return groupService.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить группу по идентификатору")
    public GroupDto get(@PathVariable Long id) {
        return groupService.get(id);
    }

    @PostMapping
    @Operation(summary = "Создать новую группу")
    public GroupDto create(@Valid @RequestBody GroupRequest request) {
        return groupService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить группу")
    public GroupDto update(@PathVariable Long id, @Valid @RequestBody GroupRequest request) {
        return groupService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить группу")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        groupService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
