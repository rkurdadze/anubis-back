package ge.comcom.anubis.repository.core;

import ge.comcom.anubis.entity.core.FileBinaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileBinaryRepository extends JpaRepository<FileBinaryEntity, Long> {

    /**
     * Finds an existing binary by its SHA-256 hash.
     * Useful for deduplication and import tasks.
     */
    Optional<FileBinaryEntity> findBySha256(String sha256);

    @Query("""
                SELECT b.id 
                FROM FileBinaryEntity b 
                LEFT JOIN ObjectFileEntity f ON f.binary.id = b.id
                WHERE f.id IS NULL
            """)
    List<Long> findOrphans();
}