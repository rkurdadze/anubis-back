package ge.comcom.anubis.repository.core;

import ge.comcom.anubis.entity.core.ObjectVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ObjectVersionRepository extends JpaRepository<ObjectVersionEntity, Long> {

    /**
     * Возвращает все версии объекта по его ID, по возрастанию номера версии.
     */
    List<ObjectVersionEntity> findByObject_IdOrderByVersionNumberAsc(Long objectId);

    /**
     * Возвращает последнюю (максимальную) версию объекта по его ID.
     */
    Optional<ObjectVersionEntity> findTopByObject_IdOrderByVersionNumberDesc(Long objectId);

    /**
     * Возвращает максимальный номер версии для объекта.
     */
    @Query("SELECT MAX(v.versionNumber) FROM ObjectVersionEntity v WHERE v.object.id = :objectId")
    Integer findLastVersionNumber(@Param("objectId") Long objectId);
}
