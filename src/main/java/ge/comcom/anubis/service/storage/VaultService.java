package ge.comcom.anubis.service.storage;

import ge.comcom.anubis.entity.core.FileStorageEntity;
import ge.comcom.anubis.entity.core.ObjectEntity;
import ge.comcom.anubis.entity.core.VaultEntity;
import ge.comcom.anubis.repository.core.FileStorageRepository;
import ge.comcom.anubis.repository.core.VaultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for resolving vaults and their default storage configurations.
 * <p>
 * Each vault can define its own default storage backend (DB / FS / S3)
 * via the {@code default_storage_id} column.
 * If the vault has no storage configured or it's inactive,
 * fallback to global default storage (file_storage.is_default = true).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VaultService {

    private final VaultRepository vaultRepository;
    private final FileStorageRepository storageRepository;

    /**
     * Retrieves an active vault by ID.
     *
     * @throws IllegalArgumentException if vault not found or inactive.
     */
    public VaultEntity getVaultById(Long vaultId) {
        VaultEntity vault = vaultRepository.findById(vaultId)
                .orElseThrow(() -> new IllegalArgumentException("Vault not found: " + vaultId));

        if (!vault.isActive()) {
            throw new IllegalStateException("Vault is inactive: " + vault.getName());
        }

        return vault;
    }

    /**
     * Retrieves vault by unique code (case-insensitive).
     */
    public VaultEntity getVaultByCode(String code) {
        return vaultRepository.findByCodeIgnoreCaseAndActiveTrue(code)
                .orElseThrow(() -> new IllegalArgumentException("Vault not found or inactive: " + code));
    }

    /**
     * Resolves which file storage backend should be used for a given object.
     * <p>
     * - If the object's vault defines a default storage and it's active → use it.
     * - Otherwise, fallback to the global default storage (is_default = true).
     */
    public FileStorageEntity resolveStorageForObject(ObjectEntity object) {
        if (object == null) {
            throw new IllegalArgumentException("Object cannot be null when resolving storage");
        }

        if (object.getVault() != null) {
            VaultEntity vault = object.getVault();
            FileStorageEntity storage = vault.getDefaultStorage();

            if (storage != null && Boolean.TRUE.equals(storage.isActive())) {
                log.debug("Resolved storage '{}' for vault '{}'", storage.getName(), vault.getName());
                return storage;
            } else {
                log.warn("Vault '{}' has no active default storage — falling back to global default", vault.getName());
            }
        }

        // fallback: global default storage
        return storageRepository.findByDefaultStorageTrue()
                .orElseThrow(() -> new IllegalStateException("No default file storage configured"));
    }
}
