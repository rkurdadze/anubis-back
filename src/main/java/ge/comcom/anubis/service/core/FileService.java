package ge.comcom.anubis.service.core;

import ge.comcom.anubis.dto.ObjectFileDto;
import ge.comcom.anubis.entity.core.FileStorageEntity;
import ge.comcom.anubis.entity.core.ObjectFileEntity;
import ge.comcom.anubis.entity.core.ObjectVersionEntity;
import ge.comcom.anubis.enums.VersionChangeType;
import ge.comcom.anubis.mapper.ObjectFileMapper;
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
import java.util.Objects;

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
    private final StorageStrategyRegistry strategyRegistry;
    private final FullTextSearchService fullTextSearchService;
    private final ObjectFileMapper objectFileMapper;

    /**
     * Returns all files attached to a given object.
     */
    public List<ObjectFileDto> getFilesByObject(Long objectId) {
        return fileRepository.findByVersionObjectIdOrderByVersionCreatedAtDesc(objectId).stream()
                .map(objectFileMapper::toDto)
                .toList();
    }

    /**
     * Returns all files attached to a specific object version.
     */
    public List<ObjectFileDto> getFilesByVersion(Long versionId) {
        return fileRepository.findByVersion_Id(versionId).stream()
                .map(objectFileMapper::toDto)
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
     * Creates or updates a logical link between a file metadata entry and a version.
     * Allows clients to attach existing binary content or rename metadata without uploading new content.
     */
    @Transactional
    public FileLinkResult linkFileToVersion(ObjectFileDto dto) {
        if (dto.getVersionId() == null) {
            throw new IllegalArgumentException("versionId is required to link file metadata");
        }

        ObjectVersionEntity targetVersion = versionService.getById(dto.getVersionId());
        ObjectFileEntity entity;
        boolean created;
        VersionChangeType changeType;

        if (dto.getId() != null) {
            entity = getFile(dto.getId());
            created = false;
            changeType = VersionChangeType.FILE_UPDATED;
        } else {
            entity = new ObjectFileEntity();
            created = true;
            changeType = VersionChangeType.FILE_ADDED;
        }

        entity.setVersion(targetVersion);

        if (dto.getFilename() != null && !dto.getFilename().isBlank()) {
            entity.setFileName(dto.getFilename());
        }

        if (entity.getFileName() == null || entity.getFileName().isBlank()) {
            throw new IllegalArgumentException("filename is required for file metadata");
        }

        if (dto.getMimeType() != null) {
            entity.setMimeType(dto.getMimeType());
        }

        if (dto.getSize() != null) {
            entity.setFileSize(dto.getSize());
        }

        if (entity.getStorage() == null) {
            FileStorageEntity storage = vaultService.resolveStorageForObject(targetVersion.getObject());
            if (storage != null) {
                entity.setStorage(storage);
            }
        }

        ObjectFileEntity saved = fileRepository.save(entity);

        auditService.logAction(
                targetVersion,
                changeType,
                UserContext.getCurrentUser().getId(),
                created
                        ? "Linked file metadata: " + saved.getFileName()
                        : "Updated file metadata: " + saved.getFileName()
        );

        return new FileLinkResult(objectFileMapper.toDto(saved), created);
    }

    /**
     * Saves a file for a given object.
     * Determines storage based on the vault configuration of the object.
     */
    @Transactional
    public ObjectFileDto saveFile(Long objectId, MultipartFile file) throws IOException {
        var user = UserContext.getCurrentUser();

        // 1. Получаем объект
        var objectEntity = objectService.getById(objectId);
        var objectType = objectEntity.getObjectType();
        if (objectType == null || objectType.getVault() == null) {
            throw new IllegalStateException("Object type for object " + objectId + " has no vault assigned");
        }

        // 2. Получаем vault и storage
        var vault = vaultService.getVaultById(objectType.getVault().getId());
        if (vault == null) {
            throw new IllegalStateException("Vault not found: " + objectType.getVault().getId());
        }

        FileStorageEntity storage = vaultService.resolveStorageForObject(objectEntity);
        if (storage == null) {
            throw new IllegalStateException("No storage configured for vault: " + vault.getName());
        }

        var strategy = strategyRegistry.resolve(storage);

        ObjectFileEntity entity = new ObjectFileEntity();
        entity.setFileName(file.getOriginalFilename());
        entity.setMimeType(file.getContentType());
        entity.setFileSize(file.getSize());
        entity.setStorage(storage);

        ObjectVersionEntity version = null;
        ObjectFileEntity savedFile = null;

        try {
            // 3. СНАЧАЛА СОХРАНЯЕМ ФАЙЛ (критическая операция)
            strategy.save(storage, entity, file);

            // 4. ТОЛЬКО ПОСЛЕ УСПЕХА — создаём версию
            version = versionService.createNewVersion(objectId, "Auto-version from upload");

            // 5. Привязываем файл к версии
            entity.setVersion(version);
            savedFile = fileRepository.save(entity);

            // 6. Асинхронная индексация
            triggerAsyncIndexing(savedFile);

            // 7. Успешный аудит
            auditService.logAction(
                    version,
                    VersionChangeType.FILE_ADDED,
                    user.getId(),
                    "File uploaded: " + entity.getFileName()
            );

            log.info(
                    "File '{}' uploaded by '{}' (object={}, version={}, vault={}, storage={})",
                    entity.getFileName(), user.getUsername(), objectId,
                    version.getVersionNumber(), vault.getName(), storage.getKind()
            );

            return objectFileMapper.toDto(savedFile);

        } catch (Exception e) {
            // 8. ОШИБКА: откат + аудит
            log.error("Failed to upload file '{}': {}", file.getOriginalFilename(), e.getMessage(), e);

            // Удаляем файл, если он был частично сохранён
            if (entity.getExternalFilePath() != null || entity.isInline()) {
                try {
                    strategy.delete(entity);
                } catch (Exception deleteEx) {
                    log.warn("Failed to cleanup partially saved file: {}", deleteEx.getMessage());
                }
            }

            // Удаляем версию, если она была создана
            if (version != null) {
                try {
                    versionService.deleteVersion(version.getId()); // должен быть @Transactional
                } catch (Exception deleteVerEx) {
                    log.error("Failed to delete orphaned version {}: {}", version.getId(), deleteVerEx.getMessage());
                }

                // Аудит ошибки сохраняем только если версия существует
                auditService.logAction(
                        version,
                        VersionChangeType.FILE_UPLOAD_FAILED,
                        user.getId(),
                        "File upload failed: " + file.getOriginalFilename() + " | Error: " + e.getMessage()
                );
            } else {
                log.warn("File upload failed before version creation for object {}: {}", objectId, e.getMessage());
            }

            // Пробрасываем исключение — клиент должен знать
            throw new IOException("Failed to save file: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public FileDownload loadFile(Long fileId) throws IOException {
        ObjectFileEntity file = getFile(fileId);
        var strategy = strategyRegistry.resolve(file.getStorage());
        byte[] data = strategy.load(file);
        if (data == null) {
            throw new IOException("Storage strategy returned null content for file " + fileId);
        }
        return new FileDownload(file, data);
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

        triggerAsyncIndexing(updated);

        auditService.logAction(
                file.getVersion(),
                VersionChangeType.FILE_UPDATED,
                user.getId(),
                "File updated: " + file.getFileName()
        );

        log.info("File '{}' updated by {}", file.getFileName(), user.getUsername());
        return objectFileMapper.toDto(updated);
    }

    private void triggerAsyncIndexing(ObjectFileEntity fileEntity) {
        try {
            fullTextSearchService.indexObjectFile(fileEntity);
        } catch (Exception ex) {
            log.error("Failed to schedule indexing for file {}: {}", fileEntity.getId(), ex.getMessage(), ex);
            // Можно добавить аудит: "Indexing failed"
        }
    }

    public record FileLinkResult(ObjectFileDto file, boolean created) { }

    public static class FileDownload {
        private final ObjectFileEntity file;
        private final byte[] content;

        public FileDownload(ObjectFileEntity file, byte[] content) {
            this.file = Objects.requireNonNull(file, "file");
            this.content = Objects.requireNonNull(content, "content");
        }

        public ObjectFileEntity getFile() {
            return file;
        }

        public byte[] getContent() {
            return content;
        }
    }

}
