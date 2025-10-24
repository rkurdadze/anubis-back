package ge.comcom.anubis.repository.core;

import ge.comcom.anubis.entity.core.ObjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Repository interface for managing ObjectEntity records.
 */
public interface ObjectRepository extends JpaRepository<ObjectEntity, Long> {

    List<ObjectEntity> findByNameContainingIgnoreCase(String name);

    List<ObjectEntity> findByTypeId(Long typeId);

    List<ObjectEntity> findByIsArchivedFalse();
}
