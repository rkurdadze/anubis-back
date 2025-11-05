package ge.comcom.anubis.repository.meta;

import ge.comcom.anubis.entity.core.PropertyValue;
import ge.comcom.anubis.entity.core.ObjectEntity;
import ge.comcom.anubis.entity.meta.PropertyDef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PropertyValueRepository extends JpaRepository<PropertyValue, Long> {

    List<PropertyValue> findAllByObjectVersionId(Long versionId);

    Optional<PropertyValue> findByObjectVersionIdAndPropertyDefId(Long versionId, Long propertyDefId);


    @Query("""
    SELECT pv
    FROM PropertyValue pv
    WHERE pv.objectVersion.object = :object
      AND pv.propertyDef = :propertyDef
""")
    Optional<PropertyValue> findByObjectAndPropertyDef(ObjectEntity object, PropertyDef propertyDef);


    @Query("""
        SELECT CASE WHEN COUNT(v) > 0 THEN TRUE ELSE FALSE END
        FROM PropertyValue v
        WHERE v.propertyDef.id = :propertyDefId
          AND v.valueText = :valueText
          AND v.objectVersion.object.objectClass.id = :classId
          AND v.objectVersion.id <> :excludeVersionId
    """)
    boolean existsDuplicateInClass(Long classId, Long propertyDefId, String valueText, Long excludeVersionId);


    List<PropertyValue> findAllByObjectVersion_Object_IdAndPropertyDef_Id(Long objectId, Long propertyDefId);
    Optional<PropertyValue> findFirstByObjectVersion_Object_IdAndPropertyDef_Id(Long objectId, Long propertyDefId);

    List<PropertyValue> findAllByObjectVersionIdAndPropertyDefId(Long versionId, Long propertyDefId);

}
