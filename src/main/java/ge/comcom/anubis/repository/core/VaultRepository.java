package ge.comcom.anubis.repository.core;

import ge.comcom.anubis.entity.core.VaultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link VaultEntity}.
 * <p>
 * Provides access to the "vault" table defined in the extended baseline migration.
 * Each vault defines its own default file storage backend via {@code default_storage_id}.
 *
 * <pre>
 * vault (
 *   vault_id SERIAL PRIMARY KEY,
 *   code TEXT UNIQUE NOT NULL,
 *   name TEXT NOT NULL,
 *   description TEXT,
 *   default_storage_id INT REFERENCES file_storage(storage_id),
 *   is_active BOOLEAN DEFAULT TRUE
 * )
 * </pre>
 */
@Repository
public interface VaultRepository extends JpaRepository<VaultEntity, Long> {

    /**
     * Finds an active vault by unique code (case-insensitive).
     */
    Optional<VaultEntity> findByCodeIgnoreCaseAndActiveTrue(String code);

    /**
     * Finds vault by ID only if active.
     */
    Optional<VaultEntity> findByIdAndActiveTrue(Long id);

    /**
     * Finds all active vaults (is_active = true).
     */
    List<VaultEntity> findByActiveTrue();

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    boolean existsByDefaultStorage_IdAndActiveTrue(Long storageId);
}
