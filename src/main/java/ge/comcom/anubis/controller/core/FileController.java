package ge.comcom.anubis.controller.core;

import ge.comcom.anubis.dto.ObjectFileDto;
import ge.comcom.anubis.service.core.DocumentPreviewService;
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
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "Upload, download, and manage object files")
public class FileController {

    private final FileService fileService;
    private final DocumentPreviewService documentPreviewService;

    // ================================================================
    // List files by object ID
    // ================================================================
    @Operation(
            summary = "Get files for object",
            description = "Returns all files associated with a given object.",
            parameters = @Parameter(name = "objectId", description = "ID of the object", example = "5"),
            responses = @ApiResponse(responseCode = "200", description = "Files retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ObjectFileDto.class)))
    )
    @GetMapping("/object/{objectId}")
    public List<ObjectFileDto> getFilesByObject(@PathVariable Long objectId) {
        return fileService.getFilesByObject(objectId);
    }

    @Operation(
            summary = "Get files for version",
            description = "Returns all files associated with a specific object version.",
            parameters = @Parameter(name = "versionId", description = "ID of the version", example = "18"),
            responses = @ApiResponse(responseCode = "200", description = "Files retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ObjectFileDto.class)))
    )
    @GetMapping("/version/{versionId}")
    public List<ObjectFileDto> getFilesByVersion(@PathVariable Long versionId) {
        return fileService.getFilesByVersion(versionId);
    }

    // ================================================================
    // Download file
    // ================================================================
    @Operation(
            summary = "Download file by ID",
            description = "Returns the file content as binary stream for download.",
            parameters = @Parameter(name = "fileId", description = "File ID", example = "12"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "File downloaded successfully",
                            content = @Content(mediaType = "application/octet-stream")),
                    @ApiResponse(responseCode = "404", description = "File not found")
            }
    )
    @GetMapping("/{fileId}/download")
    public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable Long fileId) {
        try {
            FileService.FileDownload download = fileService.loadFile(fileId);
            var file = download.getFile();

            ByteArrayResource resource = new ByteArrayResource(download.getContent());
            String filename = file.getFileName();
            String safeFilename = (filename == null || filename.isBlank()) ? "file" : filename;
            String mimeType = file.getMimeType();
            long contentLength = file.getFileSize() != null
                    ? file.getFileSize()
                    : download.getContent().length;

            ContentDisposition contentDisposition = ContentDisposition.attachment()
                    .filename(safeFilename, StandardCharsets.UTF_8)
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                    .contentType(mimeType != null
                            ? MediaType.parseMediaType(mimeType)
                            : MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(contentLength)
                    .body(resource);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ================================================================
    // Preview file as PDF
    // ================================================================
    @Operation(
            summary = "Preview file as PDF",
            description = "Конвертирует исходный файл при помощи Gotenberg и возвращает PDF для предпросмотра.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Preview rendered successfully"),
                    @ApiResponse(responseCode = "404", description = "File not found"),
                    @ApiResponse(responseCode = "502", description = "Gotenberg conversion failed"),
                    @ApiResponse(responseCode = "500", description = "Unexpected error during conversion")
            }
    )
    @GetMapping("/{fileId}/preview")
    public ResponseEntity<ByteArrayResource> previewFile(@PathVariable Long fileId) {
        try {
            DocumentPreviewService.PreviewDocument preview = documentPreviewService.renderPreview(fileId);
            ByteArrayResource resource = preview.asResource();
            ContentDisposition contentDisposition = ContentDisposition.inline()
                    .filename(preview.filename(), StandardCharsets.UTF_8)
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(preview.content().length)
                    .body(resource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ================================================================
    // Upload file and create version automatically
    // ================================================================
    @Operation(
            summary = "Upload new file",
            description = "Uploads a file and automatically creates a new version for the associated object.",
            parameters = @Parameter(name = "objectId", description = "Associated object ID", example = "5"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "File to upload (multipart/form-data)",
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            ),
            responses = @ApiResponse(responseCode = "201", description = "File uploaded successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ObjectFileDto.class)))
    )
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ObjectFileDto> uploadFile(
            @RequestParam("objectId") Long objectId,
            @RequestParam("file") MultipartFile file) throws IOException {

        ObjectFileDto saved = fileService.saveFile(objectId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ================================================================
    // Link or update file metadata without uploading content
    // ================================================================
    @Operation(
            summary = "Link file metadata to version",
            description = "Creates or updates file metadata entry for the specified version without uploading binary content.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "File metadata payload",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ObjectFileDto.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Metadata created"),
                    @ApiResponse(responseCode = "200", description = "Metadata updated"),
                    @ApiResponse(responseCode = "400", description = "Validation error")
            }
    )
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ObjectFileDto> linkFileMetadata(@RequestBody ObjectFileDto request) {
        try {
            FileService.FileLinkResult result = fileService.linkFileToVersion(request);
            HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
            return ResponseEntity.status(status).body(result.file());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ================================================================
    // Update existing file content
    // ================================================================
    @Operation(
            summary = "Update file content",
            description = "Replaces the binary content of an existing file entry.",
            parameters = @Parameter(name = "fileId", description = "File ID", example = "12"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "New file content (multipart/form-data)",
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "File updated successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ObjectFileDto.class))),
                    @ApiResponse(responseCode = "404", description = "File not found")
            }
    )
    @PutMapping(value = "/{fileId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ObjectFileDto> updateFile(
            @PathVariable Long fileId,
            @RequestParam("file") MultipartFile file) throws IOException {
        try {
            ObjectFileDto updated = fileService.updateFile(fileId, file);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ================================================================
    // Delete file
    // ================================================================
    @Operation(
            summary = "Delete file",
            description = "Removes file metadata and binary content.",
            parameters = @Parameter(name = "fileId", description = "File ID", example = "12"),
            responses = {
                    @ApiResponse(responseCode = "204", description = "File deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "File not found")
            }
    )
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long fileId) {
        try {
            fileService.deleteFile(fileId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
