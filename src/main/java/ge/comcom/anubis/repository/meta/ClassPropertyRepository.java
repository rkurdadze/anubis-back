package ge.comcom.anubis.repository.meta;

import ge.comcom.anubis.entity.meta.ClassProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ClassPropertyRepository extends JpaRepository<ClassProperty, Long> {

    List<ClassProperty> findAllByObjectClassIdOrderByDisplayOrderAsc(Long classId);

    boolean existsByObjectClassIdAndPropertyDefId(Long classId, Long propertyDefId);

    boolean existsByObjectClassIdAndPropertyDefIdAndIdNot(Long classId, Long propertyDefId, Long excludeId);
}
