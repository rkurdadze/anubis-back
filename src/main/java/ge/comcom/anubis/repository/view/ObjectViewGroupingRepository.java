package ge.comcom.anubis.repository.view;

import ge.comcom.anubis.entity.view.ObjectViewGroupingEntity;
import ge.comcom.anubis.entity.view.ObjectViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ObjectViewGroupingRepository extends JpaRepository<ObjectViewGroupingEntity, ObjectViewEntity> {

    List<ObjectViewGroupingEntity> findAllByView_IdOrderByLevelAsc(Long viewId);
}
