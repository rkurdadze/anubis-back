package ge.comcom.anubis.repository.meta;

import ge.comcom.anubis.entity.meta.ClassProperty;
import ge.comcom.anubis.entity.meta.ClassPropertyId;
import ge.comcom.anubis.entity.core.ObjectClass;
import ge.comcom.anubis.entity.meta.PropertyDef;
import ge.comcom.anubis.repository.BaseActiveRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ClassPropertyRepository extends BaseActiveRepository<ClassProperty, ClassPropertyId> {

    List<ClassProperty> findAllByObjectClass_IdOrderByDisplayOrderAsc(Long classId);

    boolean existsByObjectClassAndPropertyDef(ObjectClass objectClass,
                                              PropertyDef propertyDef);

    boolean existsByObjectClass_IdAndPropertyDef_Id(Long classId, Long propertyDefId);
}
