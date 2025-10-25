package ge.comcom.anubis.repository.core;

import ge.comcom.anubis.entity.core.FileStorageEntity;
import ge.comcom.anubis.enums.StorageKindEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link FileStorageEntity}.
 * <p>
 * Provides access to the "file_storage" table defined in the baseline schema.
 * Allows to find storages by kind, name, and default status.
 *
 * <pre>
 * file_storage (
 *   storage_id SERIAL PRIMARY KEY,
 *   kind storage_kind_enum NOT NULL,
 *   name TEXT,
 *   base_path TEXT,
 *   bucket TEXT,
 *   endpoint TEXT,
 *   access_key TEXT,
 *   secret_key TEXT,
 *   is_default BOOLEAN,
 *   description TEXT,
 *   is_active BOOLEAN
 * )
 * </pre>
 */
@Repository
public interface FileStorageRepository extends JpaRepository<FileStorageEntity, Long> {

    /**
     * Finds all storages by type (DB, FS, S3).
     *
     * @param kind storage type
     * @return list of storages
     */
    List<FileStorageEntity> findByKind(StorageKindEnum kind);

    /**
     * Finds all active storages.
     */
    List<FileStorageEntity> findByIsActiveTrue();

    /**
     * Finds the default storage (is_default = true).
     */
    Optional<FileStorageEntity> findByIsDefaultTrue();

    /**
     * Finds an active storage by name (case-insensitive).
     */
    @Query("select s from FileStorageEntity s where lower(s.name) = lower(?1) and s.isActive = true")
    Optional<FileStorageEntity> findActiveByName(String name);
}
