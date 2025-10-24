package ge.comcom.anubis.repository.core;

import ge.comcom.anubis.entity.core.ObjectFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ObjectFileRepository extends JpaRepository<ObjectFileEntity, Long> {
    @Query("""
        SELECT f FROM ObjectFileEntity f
        WHERE f.version.object.id = :objectId
        ORDER BY f.uploadedAt DESC
    """)
    List<ObjectFileEntity> findByObjectId(@Param("objectId") Long objectId);

    List<ObjectFileEntity> findByVersionId(Long versionId);
}
