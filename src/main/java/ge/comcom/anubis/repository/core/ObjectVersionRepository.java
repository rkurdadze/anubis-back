package ge.comcom.anubis.repository.core;

import ge.comcom.anubis.entity.core.ObjectVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Repository for accessing and managing ObjectVersionEntity records.
 */
public interface ObjectVersionRepository extends JpaRepository<ObjectVersionEntity, Long> {

    /**
     * Returns all versions of a specific object, ordered ascending by version number.
     *
     * @param objectId ID of the parent object
     * @return list of ObjectVersionEntity
     */
    List<ObjectVersionEntity> findByObjectIdOrderByVersionNumberAsc(Long objectId);

    /**
     * Returns the latest (highest-numbered) version for a given object.
     *
     * @param objectId ID of the parent object
     * @return optional ObjectVersionEntity
     */
    Optional<ObjectVersionEntity> findTopByObjectIdOrderByVersionNumberDesc(Long objectId);

    /**
     * Returns the highest version number for a given object.
     *
     * @param objectId ID of the parent object
     * @return last version number or null if no versions exist
     */
    @Query("SELECT MAX(v.versionNumber) FROM ObjectVersionEntity v WHERE v.objectId = :objectId")
    Integer findLastVersionNumber(Long objectId);
}
