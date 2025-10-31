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
 * Provides access to the "file_storage" table.
 */
@Repository
public interface FileStorageRepository extends JpaRepository<FileStorageEntity, Long> {

    /** Finds all storages by type (DB, FS, S3). */
    List<FileStorageEntity> findByKind(StorageKindEnum kind);

    /** Finds all active storages. */
    List<FileStorageEntity> findByActiveTrue();

    /** Finds the default storage (is_default = true). */
    Optional<FileStorageEntity> findByDefaultStorageTrue();

    /** Finds an active storage by name (case-insensitive). */
    @Query("select s from FileStorageEntity s where lower(s.name) = lower(?1) and s.active = true")
    Optional<FileStorageEntity> findActiveByName(String name);
}
