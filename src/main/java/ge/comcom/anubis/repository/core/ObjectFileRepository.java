package ge.comcom.anubis.repository.core;

import ge.comcom.anubis.entity.core.ObjectFileEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ObjectFileRepository extends JpaRepository<ObjectFileEntity, Long> {

    @EntityGraph(attributePaths = {"version", "version.object"})
    List<ObjectFileEntity> findByVersion_Id(Long versionId);

    @EntityGraph(attributePaths = {"version", "version.object"})
    List<ObjectFileEntity> findByVersionObjectIdOrderByVersionCreatedAtDesc(Long objectId);
}
