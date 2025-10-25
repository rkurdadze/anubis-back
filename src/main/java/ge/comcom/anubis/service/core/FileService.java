package ge.comcom.anubis.service.core;

import ge.comcom.anubis.dto.ObjectFileDto;
import ge.comcom.anubis.entity.core.FileStorageEntity;
import ge.comcom.anubis.entity.core.ObjectFileEntity;
import ge.comcom.anubis.entity.core.ObjectVersionEntity;
import ge.comcom.anubis.enums.VersionChangeType;
import ge.comcom.anubis.repository.core.FileStorageRepository;
import ge.comcom.anubis.repository.core.ObjectFileRepository;
import ge.comcom.anubis.service.storage.StorageStrategyRegistry;
import ge.comcom.anubis.service.storage.VaultService;
import ge.comcom.anubis.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Service for managing object files and linking them with versions.
 * <p>
 * Automatically determines storage from vault configuration.
 * Each vault may have different physical storage backend (DB, FS, S3).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final ObjectFileRepository fileRepository;
    private final ObjectVersionService versionService;
    private final ObjectService objectService; // 🔹 новый сервис для доступа к объектам
    private final VaultService vaultService;   // 🔹 для определения vault → storage
    private final ObjectVersionAuditService auditService;
    private final FileStorageRepository storageRepository;
    private final StorageStrategyRegistry strategyRegistry;

    /**
     * Returns all files attached to a given object.
     */
    public List<ObjectFileDto> getFilesByObject(Long objectId) {
        return fileRepository.findByVersionObjectIdOrderByVersionCreatedAtDesc(objectId).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Retrieves a file entity by ID.
     */
    public ObjectFileEntity getFile(Long id) {
        return fileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + id));
    }

    /**
     * Saves a file for a given object.
     * Determines storage based on the vault configuration of the object.
     */
    @Transactional
    public ObjectFileDto saveFile(Long objectId, MultipartFile file) throws IOException {
        var user = UserContext.getCurrentUser();

        // 1️⃣ Get object and resolve its vault
        var objectEntity = objectService.getObjectById(objectId);
        var vault = vaultService.getVaultById(objectEntity.getVault().getId());

        // 2️⃣ Resolve file storage from vault
        FileStorageEntity storage = vault.getDefaultStorage();
        if (storage == null) {
            throw new IllegalStateException("No storage configured for vault: " + vault.getName());
        }


        // 3️⃣ Create new version
        ObjectVersionEntity version = versionService.createNewVersion(objectId, "Auto-version from upload");

        // 4️⃣ Prepare file entity
        ObjectFileEntity entity = new ObjectFileEntity();
        entity.setVersion(version);
        entity.setFileName(file.getOriginalFilename());
        entity.setMimeType(file.getContentType());
        entity.setFileSize(file.getSize());
        entity.setStorage(storage);

        // 5️⃣ Apply appropriate storage strategy (DB, FS, S3)
        var strategy = strategyRegistry.resolve(storage);
        strategy.save(storage, entity, file);

        // 6️⃣ Persist metadata
        ObjectFileEntity saved = fileRepository.save(entity);

        // 7️⃣ Log audit
        auditService.logAction(
                version,
                VersionChangeType.FILE_ADDED,
                user.getId(),
                "File uploaded: " + entity.getFileName()
        );

        log.info(
                "File '{}' uploaded by '{}' (object={}, version={}, vault={}, storage={})",
                entity.getFileName(),
                user.getUsername(),
                objectId,
                version.getVersionNumber(),
                vault.getName(),
                storage.getKind()
        );

        return toDto(saved);
    }

    @Transactional
    public void deleteFile(Long fileId) {
        ObjectFileEntity file = getFile(fileId);
        var strategy = strategyRegistry.resolve(file.getStorage());
        var user = UserContext.getCurrentUser();

        try {
            strategy.delete(file);
        } catch (IOException e) {
            log.error("Failed to delete file content for '{}': {}", file.getFileName(), e.getMessage());
        }

        fileRepository.delete(file);

        auditService.logAction(
                file.getVersion(),
                VersionChangeType.FILE_REMOVED,
                user.getId(),
                "File deleted: " + file.getFileName()
        );

        log.warn("File '{}' deleted by {}", file.getFileName(), user.getUsername());
    }

    @Transactional
    public ObjectFileDto updateFile(Long fileId, MultipartFile newFile) throws IOException {
        ObjectFileEntity file = getFile(fileId);
        var strategy = strategyRegistry.resolve(file.getStorage());
        var user = UserContext.getCurrentUser();

        strategy.save(file.getStorage(), file, newFile);

        file.setFileName(newFile.getOriginalFilename());
        file.setMimeType(newFile.getContentType());
        file.setFileSize(newFile.getSize());

        ObjectFileEntity updated = fileRepository.save(file);

        auditService.logAction(
                file.getVersion(),
                VersionChangeType.FILE_UPDATED,
                user.getId(),
                "File updated: " + file.getFileName()
        );

        log.info("File '{}' updated by {}", file.getFileName(), user.getUsername());
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
                .build();
    }
}
