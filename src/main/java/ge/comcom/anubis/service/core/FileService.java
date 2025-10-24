package ge.comcom.anubis.service.core;

import ge.comcom.anubis.dto.ObjectFileDto;
import ge.comcom.anubis.entity.core.ObjectFileEntity;
import ge.comcom.anubis.entity.core.ObjectVersionEntity;
import ge.comcom.anubis.enums.VersionChangeType;
import ge.comcom.anubis.repository.core.ObjectFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * Service for managing object files and linking them with versions.
 * Includes automatic version creation and audit logging.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final ObjectFileRepository fileRepository;
    private final ObjectVersionService versionService;
    private final ObjectVersionAuditService auditService;

    /**
     * Returns all files attached to a given object.
     */
    public List<ObjectFileDto> getFilesByObject(Long objectId) {
        return fileRepository.findByObjectId(objectId).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Retrieves a file entity by ID.
     */
    public ObjectFileEntity getFile(Long id) {
        return fileRepository.findById(id).orElse(null);
    }

    /**
     * Saves a new file for the object (automatically creates a new version).
     * Logs FILE_ADDED audit record.
     */
    public ObjectFileDto saveFile(Long objectId, MultipartFile file) throws IOException {
        // Auto-create new version
        ObjectVersionEntity version = versionService.createNewVersion(objectId, "Auto-version from upload");

        ObjectFileEntity entity = new ObjectFileEntity();
        entity.setVersion(version);
        entity.setFileName(file.getOriginalFilename());
        entity.setMimeType(file.getContentType());
        entity.setFileSize(file.getSize());
        entity.setUploadedBy("system"); // TODO: replace with authenticated user
        entity.setUploadedAt(Instant.now());
        entity.setContent(file.getBytes());

        ObjectFileEntity saved = fileRepository.save(entity);

        // Log audit
        auditService.logAction(
                version,
                VersionChangeType.FILE_ADDED,
                null, // TODO: user_id
                "File uploaded: " + entity.getFileName()
        );

        log.info("File uploaded and version {} created for object {}", version.getVersionNumber(), objectId);
        return toDto(saved);
    }

    /**
     * Deletes a file and logs FILE_REMOVED in audit.
     */
    public void deleteFile(Long fileId) {
        ObjectFileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found: " + fileId));

        ObjectVersionEntity version = file.getVersion();

        fileRepository.delete(file);

        auditService.logAction(
                version,
                VersionChangeType.FILE_REMOVED,
                null,
                "File deleted: " + file.getFileName()
        );

        log.warn("File deleted: {}", file.getFileName());
    }

    /**
     * Updates an existing file (content replacement) and logs FILE_UPDATED.
     */
    public ObjectFileDto updateFile(Long fileId, MultipartFile newFile) throws IOException {
        ObjectFileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found: " + fileId));

        file.setFileName(newFile.getOriginalFilename());
        file.setMimeType(newFile.getContentType());
        file.setFileSize(newFile.getSize());
        file.setContent(newFile.getBytes());
        file.setUploadedAt(Instant.now());

        ObjectFileEntity updated = fileRepository.save(file);

        auditService.logAction(
                file.getVersion(),
                VersionChangeType.FILE_UPDATED,
                null,
                "File updated: " + file.getFileName()
        );

        log.info("File {} updated successfully", file.getFileName());
        return toDto(updated);
    }

    private ObjectFileDto toDto(ObjectFileEntity entity) {
        return ObjectFileDto.builder()
                .id(entity.getId())
                .objectId(entity.getVersion() != null && entity.getVersion().getObject() != null
                        ? entity.getVersion().getObject().getId() : null)
                .versionId(entity.getVersion() != null ? entity.getVersion().getId() : null)
                .filename(entity.getFileName())
                .mimeType(entity.getMimeType())
                .size(entity.getFileSize())
                .uploadedAt(entity.getUploadedAt().toString())
                .build();
    }
}
