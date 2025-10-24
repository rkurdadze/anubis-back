package ge.comcom.anubis.controller.view;

import ge.comcom.anubis.entity.core.ObjectVersionEntity;
import ge.comcom.anubis.service.view.ObjectViewExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoint for executing saved views (like running a virtual folder).
 */
@RestController
@RequestMapping("/api/v1/views")
@RequiredArgsConstructor
public class ObjectViewExecutionController {

    private final ObjectViewExecutionService executionService;

    @GetMapping("/{id}/execute/{userId}")
    public ResponseEntity<List<ObjectVersionEntity>> executeWithAcl(
            @PathVariable("id") Long id,
            @PathVariable("userId") Long userId) {
        List<ObjectVersionEntity> result = executionService.execute(id, userId);
        return ResponseEntity.ok(result);
    }
}
