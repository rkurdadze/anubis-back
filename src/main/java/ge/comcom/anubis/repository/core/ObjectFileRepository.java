package ge.comcom.anubis.repository.core;

import ge.comcom.anubis.entity.core.ObjectFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ObjectFileRepository extends JpaRepository<ObjectFileEntity, Long> {
    List<ObjectFileEntity> findByObjectId(Long objectId);
    List<ObjectFileEntity> findByVersionId(Long versionId);
}
