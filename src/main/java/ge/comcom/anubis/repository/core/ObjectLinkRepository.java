package ge.comcom.anubis.repository.core;

import ge.comcom.anubis.entity.core.ObjectLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ObjectLinkRepository extends JpaRepository<ObjectLinkEntity, Long> {

    List<ObjectLinkEntity> findBySource_Id(Long objectId);

    List<ObjectLinkEntity> findByTarget_Id(Long objectId);

    List<ObjectLinkEntity> findBySource_IdOrTarget_Id(Long sourceId, Long targetId);

    // 🔹 Добавлено для обратного поиска
    List<ObjectLinkEntity> findByTarget_IdAndRole_NameIgnoreCase(Long targetId, String roleName);


    List<ObjectLinkEntity> findBySource_IdAndRole_NameIgnoreCase(Long sourceId, String role);

    void deleteBySource_IdAndTarget_IdAndRole_Id(Long srcId, Long dstId, Long roleId);
}
