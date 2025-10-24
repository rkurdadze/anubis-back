package ge.comcom.anubis.controller.core;

import ge.comcom.anubis.entity.core.ObjectFile;
import ge.comcom.anubis.service.core.ObjectFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class ObjectFileController {

    private final ObjectFileService objectFileService;

    @GetMapping("/version/{versionId}")
    public List<ObjectFile> getFiles(@PathVariable Long versionId) {
        return objectFileService.getFiles(versionId);
    }

    @PostMapping
    public ObjectFile upload(@RequestBody ObjectFile file) {
        return objectFileService.save(file);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        objectFileService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
