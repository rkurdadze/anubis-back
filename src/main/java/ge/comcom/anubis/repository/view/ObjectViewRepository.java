package ge.comcom.anubis.repository.view;

import ge.comcom.anubis.entity.view.ObjectViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ObjectViewRepository extends JpaRepository<ObjectViewEntity, Long> {

    // Важно: правильный путь — createdBy.id
    List<ObjectViewEntity> findAllByCreatedBy_IdOrIsCommonTrue(Long userId);
}
