package ge.comcom.anubis.controller.core;

import ge.comcom.anubis.dto.core.ObjectFileDto;
import ge.comcom.anubis.dto.mapper.ObjectFileMapper;
import ge.comcom.anubis.entity.core.ObjectFileEntity;
import ge.comcom.anubis.service.core.ObjectFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing object file versions.
 * Provides CRUD operations for files attached to specific object versions.
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "Object Files", description = "API for managing files related to object versions")
public class ObjectFileController {

    private final ObjectFileService objectFileService;
    private final ObjectFileMapper mapper;

    /**
     * Returns all files associated with a given object version.
     *
     * @param versionId ID of the object version
     * @return List of files related to the specified version
     */
    @GetMapping("/version/{versionId}")
    @Operation(
            summary = "Get files by version ID",
            description = "Returns a list of all files linked to the specified object version."
    )
    public List<ObjectFileDto> getFiles(
            @Parameter(description = "Version ID to fetch files for", example = "101")
            @PathVariable Long versionId) {

        return objectFileService.getFiles(versionId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    /**
     * Uploads or saves a new file for an object or version.
     *
     * @param dto File DTO containing metadata and file information
     * @return Saved file DTO
     */
    @PostMapping
    @Operation(
            summary = "Save file",
            description = "Creates or updates a file entry linked to a specific object or version."
    )
    public ObjectFileDto upload(
            @Parameter(description = "File DTO with metadata and binary content reference")
            @RequestBody ObjectFileDto dto) {

        ObjectFileEntity entity = mapper.toEntity(dto);
        ObjectFileEntity saved = objectFileService.save(entity);
        return mapper.toDto(saved);
    }

    /**
     * Deletes a file by its ID.
     *
     * @param id ID of the file to delete
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete file",
            description = "Deletes a file entry from the database by its ID."
    )
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID of the file to delete", example = "42")
            @PathVariable Long id) {

        objectFileService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
