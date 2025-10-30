package ge.comcom.anubis.controller.security;

import ge.comcom.anubis.dto.UserDto;
import ge.comcom.anubis.dto.UserRequest;
import ge.comcom.anubis.service.security.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/security/users")
@RequiredArgsConstructor
@Tag(name = "Security - Users", description = "Управление пользователями")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Список пользователей")
    public List<UserDto> list() {
        return userService.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить пользователя по идентификатору")
    public UserDto get(@PathVariable Long id) {
        return userService.get(id);
    }

    @PostMapping
    @Operation(summary = "Создать нового пользователя")
    public UserDto create(@Valid @RequestBody UserRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить пользователя")
    public UserDto update(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        return userService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить пользователя")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
