package ge.comcom.anubis.repository.core;

import ge.comcom.anubis.entity.core.ObjectLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    boolean existsBySource_IdAndTarget_IdAndRole_Id(Long sourceId, Long targetId, Long roleId);

    List<ObjectLinkEntity> findBySource_IdAndTarget_IdAndRole_Id(Long sourceId, Long targetId, Long roleId);

    // В ObjectLinkRepository
    @Modifying
    @Query("DELETE FROM ObjectLinkEntity l WHERE " +
            "(l.source.id = :srcId AND l.target.id = :dstId AND l.role.id = :roleId) OR " +
            "(l.source.id = :dstId AND l.target.id = :srcId AND l.role.id = :roleId)")
    int deleteBidirectional(@Param("srcId") Long srcId, @Param("dstId") Long dstId, @Param("roleId") Long roleId);
}
