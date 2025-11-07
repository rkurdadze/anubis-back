package ge.comcom.anubis.repository.core;

import ge.comcom.anubis.entity.core.ObjectFileEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ObjectFileRepository extends JpaRepository<ObjectFileEntity, Long> {

    @EntityGraph(attributePaths = {"version", "version.object"})
    List<ObjectFileEntity> findByVersion_Id(Long versionId);

    @EntityGraph(attributePaths = {"version", "version.object"})
    List<ObjectFileEntity> findByVersionObjectIdOrderByVersionCreatedAtDesc(Long objectId);

    @EntityGraph(attributePaths = {"version", "version.object"})
    @Query("""
            SELECT f FROM ObjectFileEntity f
            WHERE (LOWER(f.mimeType) LIKE 'image/%')
               OR LOWER(f.mimeType) = 'application/pdf'
               OR LOWER(f.fileName) LIKE '%.tif'
               OR LOWER(f.fileName) LIKE '%.tiff'
               OR LOWER(f.fileName) LIKE '%.jpg'
               OR LOWER(f.fileName) LIKE '%.jpeg'
               OR LOWER(f.fileName) LIKE '%.png'
               OR LOWER(f.fileName) LIKE '%.bmp'
    """)
    List<ObjectFileEntity> findAllOcrCandidates();

    @EntityGraph(attributePaths = {"version", "version.object"})
    @Query("""
            SELECT f FROM ObjectFileEntity f
            WHERE f.version.id NOT IN (
                SELECT c.objectVersionId FROM SearchTextCache c
            )
    """)
    List<ObjectFileEntity> findAllWithoutIndexedText();
}
