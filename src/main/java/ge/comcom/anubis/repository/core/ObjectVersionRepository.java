package ge.comcom.anubis.repository.core;

import ge.comcom.anubis.entity.core.ObjectVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ObjectVersionRepository extends JpaRepository<ObjectVersionEntity, Long> {

    @Query("SELECT MAX(v.versionNumber) FROM ObjectVersionEntity v WHERE v.objectId = :objectId")
    Integer findLastVersionNumber(@Param("objectId") Long objectId);

    Optional<ObjectVersionEntity> findTopByObjectIdOrderByVersionNumberDesc(Long objectId);
}
