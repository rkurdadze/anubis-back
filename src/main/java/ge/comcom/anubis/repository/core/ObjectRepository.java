package ge.comcom.anubis.repository.core;

import ge.comcom.anubis.entity.core.ObjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing ObjectEntity records.
 */
public interface ObjectRepository extends JpaRepository<ObjectEntity, Long> {
    List<ObjectEntity> findByObjectType_Id(Long typeId);


    @Query("SELECT o FROM ObjectEntity o " +
            "LEFT JOIN FETCH o.outgoingLinks " +
            "LEFT JOIN FETCH o.incomingLinks " +
            "WHERE o.id = :id")
    Optional<ObjectEntity> findByIdWithLinks(@Param("id") Long id);

    List<ObjectEntity> findByIsDeletedFalse();

    boolean existsByVault_Id(Long vaultId);
}
