package ge.comcom.anubis.service.core;
import ge.comcom.anubis.entity.core.FileBinaryEntity;
import ge.comcom.anubis.repository.core.FileBinaryRepository;

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
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Comparator;
import java.util.stream.Collectors;

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
    private final FileBinaryRepository binaryRepository;

    private static final String DEFAULT_VERSION_COMMENT = "Auto-version from upload";

    /**
     * Returns all files attached to a given object.
     */
    @Transactional(readOnly = true)
    public List<ObjectFileDto> getFilesByObject(Long objectId) {
        var allFiles = fileRepository.findAllByObjectId(objectId);

        return allFiles.stream()
            .collect(Collectors.groupingBy(f -> f.getBinary().getId()))
            .values().stream()
            .map(list -> list.stream()
                .max(Comparator.comparing(f -> f.getVersion().getId()))
                .orElse(null))
            .filter(Objects::nonNull)
            .filter(f -> !f.isDeleted())
            .map(objectFileMapper::toDto)
            .toList();
    }

    /**
     * Returns all files attached to a specific object version.
     */
    @Transactional(readOnly = true)
    public List<ObjectFileDto> getFilesByVersion(Long versionId) {
        var targetVersion = versionService.getById(versionId);
        Long objectId = targetVersion.getObject().getId();

        var allFiles = fileRepository.findAllByObjectId(objectId);

        return allFiles.stream()
            .filter(f -> f.getVersion().getId() <= versionId)
            .collect(Collectors.groupingBy(f -> f.getBinary().getId()))
            .values().stream()
            .map(list -> list.stream()
                .max(Comparator.comparing(f -> f.getVersion().getId()))
                .orElse(null))
            .filter(Objects::nonNull)
            .filter(f -> {
                {
                    // История файла (все состояния по одному логическому файлу = binary id)
                    var history = allFiles.stream()
                        .filter(x -> Objects.equals(x.getBinary().getId(), f.getBinary().getId()))
                        .toList();

                    // Первая версия, где файл стал deleted=true
                    Long deletedAt = history.stream()
                        .filter(ObjectFileEntity::isDeleted)
                        .map(x -> x.getVersion().getId())
                        .min(Long::compareTo)
                        .orElse(null);

                    // Если файл никогда не удалялся → показываем
                    if (deletedAt == null) return true;

                    // Если удалён после выбранной версии → показываем
                    return deletedAt > versionId;
                }
            })
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
        return saveFile(objectId, file, null);
    }

    @Transactional
    public ObjectFileDto saveFile(Long objectId, MultipartFile file, SaveOptions options) throws IOException {
        var user = UserContext.getCurrentUser();
        SaveOptions effectiveOptions = options != null ? options : SaveOptions.builder().build();

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

        // Create FileBinaryEntity
        FileBinaryEntity binary = new FileBinaryEntity();
        binary.setInline(true);
        binary.setContent(file.getBytes());
        binary.setSha256(computeSha256(file.getBytes()));
        binary.setMimeType(file.getContentType());
        binary.setSize(file.getSize());
        binary.setCreatedAt(Instant.now());
        binary = binaryRepository.save(binary);

        ObjectFileEntity entity = new ObjectFileEntity();
        entity.setFileName(file.getOriginalFilename());
        entity.setBinary(binary);

        ObjectVersionEntity version = null;
        boolean versionCreatedHere = effectiveOptions.getTargetVersionId() == null;
        ObjectFileEntity savedFile = null;

        try {
            // 3. СНАЧАЛА СОХРАНЯЕМ бинарь (уже сохранён выше)

            // 4. ТОЛЬКО ПОСЛЕ УСПЕХА — создаём версию
            if (versionCreatedHere) {
                version = versionService.createNewVersion(
                        objectId,
                        effectiveOptions.getVersionComment(),
                        effectiveOptions.getVersionCreatedAt(),
                        effectiveOptions.getVersionModifiedAt()
                );
            } else {
                version = versionService.getById(effectiveOptions.getTargetVersionId());
                if (!Objects.equals(version.getObject().getId(), objectId)) {
                    throw new IllegalArgumentException("Target version does not belong to object " + objectId);
                }
            }

            // 5. Привязываем файл к версии
            entity.setVersion(version);
            savedFile = fileRepository.save(entity);

            // 6. Асинхронная индексация
            if (!effectiveOptions.isSkipIndexing()) {
                triggerAsyncIndexing(savedFile);
            }

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

            // Удаляем версию, если она была создана
            if (version != null && versionCreatedHere) {
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
        FileBinaryEntity binary = file.getBinary();
        byte[] data = binary != null ? binary.getContent() : null;
        if (data == null) {
            throw new IOException("Binary content is null for file " + fileId);
        }
        return new FileDownload(file, data);
    }

    @Transactional
    public void deleteFile(Long fileId) {
        ObjectFileEntity file = getFile(fileId);
        var user = UserContext.getCurrentUser();

        Long objectId = file.getVersion().getObject().getId();
        String fileName = file.getFileName();
        ObjectVersionEntity newVersion = versionService.createNewVersion(
                objectId,
                "File deleted: " + fileName
        );

        // Soft delete: create a new ObjectFileEntity with deleted=true, same binary, new version
        ObjectFileEntity deletedEntry = new ObjectFileEntity();
        deletedEntry.setVersion(newVersion);
        deletedEntry.setDeleted(true);
        deletedEntry.setFileName(fileName);
        deletedEntry.setBinary(file.getBinary());
        fileRepository.save(deletedEntry);

        auditService.logAction(
                newVersion,
                VersionChangeType.FILE_REMOVED,
                user.getId(),
                "File deleted: " + fileName
        );

        log.warn(
                "File '{}' deleted by {} (new version={})",
                fileName,
                user.getUsername(),
                newVersion.getVersionNumber()
        );
    }

    @Transactional
    public ObjectFileDto updateFile(Long fileId, MultipartFile newFile) throws IOException {
        ObjectFileEntity file = getFile(fileId);
        var user = UserContext.getCurrentUser();

        String incomingName = newFile.getOriginalFilename();
        String effectiveName = (incomingName != null && !incomingName.isBlank())
                ? incomingName
                : file.getFileName();

        Long objectId = file.getVersion().getObject().getId();
        ObjectVersionEntity newVersion = versionService.createNewVersion(
                objectId,
                "File updated: " + effectiveName
        );

        // Create new FileBinaryEntity for updated content
        FileBinaryEntity updatedBinary = new FileBinaryEntity();
        updatedBinary.setInline(true);
        updatedBinary.setContent(newFile.getBytes());
        updatedBinary.setSha256(computeSha256(newFile.getBytes()));
        updatedBinary.setMimeType(newFile.getContentType());
        updatedBinary.setSize(newFile.getSize());
        updatedBinary.setCreatedAt(Instant.now());
        updatedBinary = binaryRepository.save(updatedBinary);

        ObjectFileEntity updatedEntry = new ObjectFileEntity();
        updatedEntry.setVersion(newVersion);
        updatedEntry.setBinary(updatedBinary);
        updatedEntry.setFileName(effectiveName);
        ObjectFileEntity updated = fileRepository.save(updatedEntry);

        triggerAsyncIndexing(updated);

        auditService.logAction(
                newVersion,
                VersionChangeType.FILE_UPDATED,
                user.getId(),
                "File updated: " + updated.getFileName()
        );

        log.info(
                "File '{}' updated by {} (new version={})",
                updated.getFileName(),
                user.getUsername(),
                newVersion.getVersionNumber()
        );
        return objectFileMapper.toDto(updated);
    }

    public static class SaveOptions {
        private final boolean skipIndexing;
        private final Instant versionCreatedAt;
        private final Instant versionModifiedAt;
        private final String versionComment;
        private final Long targetVersionId;

        private SaveOptions(boolean skipIndexing, Instant versionCreatedAt, Instant versionModifiedAt,
                            String versionComment, Long targetVersionId) {
            this.skipIndexing = skipIndexing;
            this.versionCreatedAt = versionCreatedAt;
            this.versionModifiedAt = versionModifiedAt;
            this.versionComment = versionComment != null ? versionComment : DEFAULT_VERSION_COMMENT;
            this.targetVersionId = targetVersionId;
        }

        public static Builder builder() {
            return new Builder();
        }

        public boolean isSkipIndexing() {
            return skipIndexing;
        }

        public Instant getVersionCreatedAt() {
            return versionCreatedAt;
        }

        public Instant getVersionModifiedAt() {
            return versionModifiedAt;
        }

        public String getVersionComment() {
            return versionComment;
        }

        public Long getTargetVersionId() {
            return targetVersionId;
        }

        public static SaveOptions importOptions(Instant createdAt, Instant modifiedAt, String comment) {
            return builder()
                    .skipIndexing(true)
                    .versionCreatedAt(createdAt)
                    .versionModifiedAt(modifiedAt)
                    .versionComment(comment)
                    .build();
        }

        public static class Builder {
            private boolean skipIndexing;
            private Instant versionCreatedAt;
            private Instant versionModifiedAt;
            private String versionComment = DEFAULT_VERSION_COMMENT;
            private Long targetVersionId;

            public Builder skipIndexing(boolean skipIndexing) {
                this.skipIndexing = skipIndexing;
                return this;
            }

            public Builder versionCreatedAt(Instant versionCreatedAt) {
                this.versionCreatedAt = versionCreatedAt;
                return this;
            }

            public Builder versionModifiedAt(Instant versionModifiedAt) {
                this.versionModifiedAt = versionModifiedAt;
                return this;
            }

            public Builder versionComment(String versionComment) {
                this.versionComment = versionComment;
                return this;
            }

            public Builder targetVersionId(Long targetVersionId) {
                this.targetVersionId = targetVersionId;
                return this;
            }

            public SaveOptions build() {
                return new SaveOptions(skipIndexing, versionCreatedAt, versionModifiedAt, versionComment, targetVersionId);
            }
        }
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

    private static String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);

            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute sha256", e);
        }
    }

}
