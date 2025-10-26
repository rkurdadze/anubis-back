package ge.comcom.anubis.repository.meta;

import ge.comcom.anubis.entity.meta.ClassProperty;
import ge.comcom.anubis.repository.BaseActiveRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ClassPropertyRepository extends BaseActiveRepository<ClassProperty, Long> {

    List<ClassProperty> findAllByObjectClassIdOrderByDisplayOrderAsc(Long classId);

    boolean existsByObjectClassIdAndPropertyDefId(Long classId, Long propertyDefId);

    boolean existsByObjectClassIdAndPropertyDefIdAndIdNot(Long classId, Long propertyDefId, Long excludeId);
}
