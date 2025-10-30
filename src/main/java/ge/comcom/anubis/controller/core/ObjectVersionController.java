package ge.comcom.anubis.controller.core;

import ge.comcom.anubis.dto.ObjectVersionDto;
import ge.comcom.anubis.mapper.ObjectVersionMapper;
import ge.comcom.anubis.entity.core.ObjectVersionEntity;
import ge.comcom.anubis.service.core.ObjectVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing object versions.
 * Provides endpoints for creating and deleting object versions.
 */
@RestController
@RequestMapping("/api/v1/versions")
@RequiredArgsConstructor
@Tag(name = "Object Versions", description = "API for managing object version entities")
public class ObjectVersionController {

    private final ObjectVersionService objectVersionService;
    private final ObjectVersionMapper mapper;

    /**
     * Creates a new version for a given object.
     *
     * @param dto Version data transfer object containing version details
     * @return Created version as DTO
     */
    @PostMapping
    @Operation(
            summary = "Create a new version",
            description = "Creates a new version entity for an object based on provided data."
    )
    public ObjectVersionDto create(
            @Parameter(description = "Object version DTO to be saved")
            @RequestBody ObjectVersionDto dto) {

        ObjectVersionEntity entity = mapper.toEntity(dto);
        ObjectVersionEntity saved = objectVersionService.save(entity);
        return mapper.toDto(saved);
    }

    /**
     * Deletes a version by its ID.
     *
     * @param id ID of the version to delete
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete version",
            description = "Deletes an object version entity by its unique ID."
    )
    public ResponseEntity<Void> delete(
            @Parameter(description = "Version ID to delete", example = "10")
            @PathVariable Long id) {

        objectVersionService.deleteVersion(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Возвращает конкретную версию по её идентификатору.
     *
     * @param id идентификатор версии
     * @return DTO с данными указанной версии
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Get version by ID",
            description = "Возвращает полную информацию по указанной версии объекта."
    )
    public ObjectVersionDto getById(
            @Parameter(description = "Идентификатор версии", example = "10")
            @PathVariable Long id) {

        return objectVersionService.getVersionDto(id);
    }

    /**
     * Returns all versions for the specified object ordered by version number in descending order.
     *
     * @param objectId identifier of the object whose versions should be retrieved
     * @return list of object version DTOs
     */
    @GetMapping("/object/{objectId}")
    @Operation(
            summary = "Get versions by object",
            description = "Retrieves all versions for the given object identifier."
    )
    public List<ObjectVersionDto> getVersionsByObject(
            @Parameter(description = "Identifier of the object", example = "42")
            @PathVariable Long objectId) {

        return objectVersionService.getVersionsByObject(objectId);
    }
}
