package ge.comcom.anubis.repository.core;

import ge.comcom.anubis.entity.core.ObjectVersionAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for object version audit logs.
 */
public interface ObjectVersionAuditRepository extends JpaRepository<ObjectVersionAuditEntity, Long> {

    List<ObjectVersionAuditEntity> findByVersion_IdOrderByModifiedAtDesc(Long versionId);
}
