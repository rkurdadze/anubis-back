package ge.comcom.anubis.repository.meta;

import ge.comcom.anubis.entity.meta.ClassProperty;
import ge.comcom.anubis.entity.meta.ClassPropertyId;
import ge.comcom.anubis.entity.core.ObjectClass;
import ge.comcom.anubis.entity.meta.PropertyDef;
import ge.comcom.anubis.repository.BaseActiveRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ClassPropertyRepository extends BaseActiveRepository<ClassProperty, ClassPropertyId> {

    List<ClassProperty> findAllByObjectClass_IdOrderByDisplayOrderAsc(Long classId);

    @Query("""
            SELECT cp FROM ClassProperty cp
            JOIN FETCH cp.propertyDef pd
            JOIN FETCH cp.objectClass oc
            WHERE oc.id IN :classIds
              AND cp.isActive = true
              AND pd.isActive = true
            ORDER BY oc.id ASC, COALESCE(cp.displayOrder, 2147483647), LOWER(pd.name)
            """)
    List<ClassProperty> findAllActiveByClassIds(@Param("classIds") Collection<Long> classIds);

    boolean existsByObjectClassAndPropertyDef(ObjectClass objectClass,
                                              PropertyDef propertyDef);

    boolean existsByObjectClass_IdAndPropertyDef_Id(Long classId, Long propertyDefId);
}
