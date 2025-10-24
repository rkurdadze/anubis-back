package ge.comcom.anubis.controller.core;

import ge.comcom.anubis.dto.ObjectFileDto;
import ge.comcom.anubis.service.core.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Controller for managing files in the repository.
 * Supports upload, download and version linkage.
 */
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "Upload, download, and manage object files")
public class FileController {

    private final FileService fileService;

    // ================================================================
    // List files by object ID
    // ================================================================
    @Operation(
            summary = "Get files for object",
            description = "Returns all files associated with a given object.",
            parameters = {
                    @Parameter(name = "objectId", description = "ID of the object", example = "5")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Files retrieved successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ObjectFileDto.class)))
            }
    )
    @GetMapping("/object/{objectId}")
    public List<ObjectFileDto> getFilesByObject(@PathVariable Long objectId) {
        return fileService.getFilesByObject(objectId);
    }

    // ================================================================
    // Download file
    // ================================================================
    @Operation(
            summary = "Download file by ID",
            description = "Returns the file content as binary stream for download.",
            parameters = {
                    @Parameter(name = "fileId", description = "File ID", example = "12")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "File downloaded successfully",
                            content = @Content(mediaType = "application/octet-stream")),
                    @ApiResponse(responseCode = "404", description = "File not found")
            }
    )
    @GetMapping("/{fileId}/download")
    public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable Long fileId) {
        var file = fileService.getFile(fileId);
        if (file == null) return ResponseEntity.notFound().build();

        ByteArrayResource resource = new ByteArrayResource(file.getData());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(file.getMimeType()))
                .contentLength(file.getSize())
                .body(resource);
    }

    // ================================================================
    // Upload file and create version automatically
    // ================================================================
    @Operation(
            summary = "Upload new file",
            description = "Uploads a file and automatically creates a new version for the associated object.",
            parameters = {
                    @Parameter(name = "objectId", description = "Associated object ID", example = "5")
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "File to upload (multipart/form-data)",
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "File uploaded successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ObjectFileDto.class)))
            }
    )
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ObjectFileDto> uploadFile(
            @RequestParam("objectId") Long objectId,
            @RequestParam("file") MultipartFile file) throws IOException {

        ObjectFileDto saved = fileService.saveFile(objectId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
