package ge.comcom.anubis.repository.core;

import ge.comcom.anubis.entity.core.ObjectVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ObjectVersionRepository extends JpaRepository<ObjectVersion, Long> {
    List<ObjectVersion> findByObject_IdOrderByVersionNumDesc(Long objectId);
}
