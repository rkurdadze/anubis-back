package ge.comcom.anubis.repository.core;

import ge.comcom.anubis.entity.core.ObjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Repository interface for managing ObjectEntity records.
 */
public interface ObjectRepository
        extends JpaRepository<ObjectEntity, Long>,
        JpaSpecificationExecutor<ObjectEntity> {
    List<ObjectEntity> findByObjectType_Id(Long typeId);


    @Query("SELECT o FROM ObjectEntity o " +
            "LEFT JOIN FETCH o.outgoingLinks " +
            "LEFT JOIN FETCH o.incomingLinks " +
            "WHERE o.id = :id")
    Optional<ObjectEntity> findByIdWithLinks(@Param("id") Long id);

    List<ObjectEntity> findByIsDeletedFalse();

    Page<ObjectEntity> findByIsDeletedFalse(Pageable pageable);

    Optional<ObjectEntity> findByObjectType_IdAndObjectClass_IdAndNameIgnoreCaseAndIsDeletedFalse(
            Long objectTypeId,
            Long objectClassId,
            String name
    );


    @Query("""
       SELECT o.objectType.id, COUNT(o)
       FROM ObjectEntity o
       WHERE o.objectType IS NOT NULL
       GROUP BY o.objectType.id
       """)
    List<Object[]> countByType();
}
