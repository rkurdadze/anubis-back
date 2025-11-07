package ge.comcom.anubis.service.storage;

import ge.comcom.anubis.dto.FileStorageRequest;
import ge.comcom.anubis.entity.core.FileStorageEntity;
import ge.comcom.anubis.enums.StorageKindEnum;
import ge.comcom.anubis.repository.core.FileStorageRepository;
import ge.comcom.anubis.repository.core.VaultRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Service for managing physical file storage configurations.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileStorageAdminService {

    private final FileStorageRepository fileStorageRepository;
    private final VaultRepository vaultRepository;

    public List<FileStorageEntity> findAll() {
        return fileStorageRepository.findAll();
    }

    public FileStorageEntity findById(Long id) {
        return fileStorageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("File storage not found: " + id));
    }

    @Transactional
    public FileStorageEntity create(FileStorageRequest request) {
        validateRequest(request);

        FileStorageEntity entity = new FileStorageEntity();
        applyRequest(entity, request);

        if (entity.isDefaultStorage()) {
            enforceDatabaseDefaultForFirstStorage(entity);
            clearExistingDefault(null);
        }

        return fileStorageRepository.save(entity);
    }

    @Transactional
    public FileStorageEntity update(Long id, FileStorageRequest request) {
        validateRequest(request);

        FileStorageEntity entity = findById(id);
        boolean wasDefault = entity.isDefaultStorage();

        applyRequest(entity, request);

        if (entity.isDefaultStorage()) {
            clearExistingDefault(id);
        }

        if (!entity.isActive() && vaultRepository.existsByDefaultStorage_IdAndActiveTrue(id)) {
            throw new IllegalStateException("Cannot deactivate storage which is used as default by active vaults");
        }

        if (!entity.isDefaultStorage() && wasDefault) {
            // No additional action is required, but make sure active vaults keep their default storage
            if (vaultRepository.existsByDefaultStorage_IdAndActiveTrue(id)) {
                throw new IllegalStateException("Cannot remove default flag while active vaults rely on this storage");
            }
        }

        return fileStorageRepository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        FileStorageEntity entity = findById(id);
        if (vaultRepository.existsByDefaultStorage_IdAndActiveTrue(id)) {
            throw new IllegalStateException("Cannot delete storage assigned as default to active vaults");
        }
        if (entity.isDefaultStorage()) {
            // Ensure system-wide default flag is cleared when this storage is removed
            fileStorageRepository.findByDefaultStorageTrue()
                    .filter(existing -> existing.getId().equals(id))
                    .ifPresent(existing -> {
                        existing.setDefaultStorage(false);
                        fileStorageRepository.save(existing);
                    });
        }
        fileStorageRepository.delete(entity);
    }

    private void applyRequest(FileStorageEntity entity, FileStorageRequest request) {
        entity.setKind(request.getKind());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setBasePath(request.getBasePath());
        entity.setBucket(request.getBucket());
        entity.setEndpoint(request.getEndpoint());
        entity.setAccessKey(request.getAccessKey());
        entity.setSecretKey(request.getSecretKey());
        entity.setDefaultStorage(request.isDefaultStorage());
        entity.setActive(request.isActive());
    }

    private void validateRequest(FileStorageRequest request) {
        if (request.getKind() == StorageKindEnum.FS && !StringUtils.hasText(request.getBasePath())) {
            throw new IllegalArgumentException("Filesystem storage requires basePath");
        }
        if (request.getKind() == StorageKindEnum.S3 && (!StringUtils.hasText(request.getBucket()) || !StringUtils.hasText(request.getEndpoint()))) {
            throw new IllegalArgumentException("S3 storage requires bucket and endpoint");
        }
    }

    private void clearExistingDefault(Long excludeId) {
        fileStorageRepository.findByDefaultStorageTrue()
                .filter(existing -> excludeId == null || !existing.getId().equals(excludeId))
                .ifPresent(existing -> {
                    existing.setDefaultStorage(false);
                    fileStorageRepository.save(existing);
                });
    }

    private void enforceDatabaseDefaultForFirstStorage(FileStorageEntity entity) {
        if (!entity.isDefaultStorage()) {
            return;
        }

        if (fileStorageRepository.count() > 0) {
            return;
        }

        entity.setKind(StorageKindEnum.DB);
        entity.setBasePath(null);
        entity.setBucket(null);
        entity.setEndpoint(null);
        entity.setAccessKey(null);
        entity.setSecretKey(null);
    }
}
