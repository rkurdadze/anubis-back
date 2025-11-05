package ge.comcom.anubis.repository.core;

import ge.comcom.anubis.entity.core.PropertyValueMulti;
import ge.comcom.anubis.entity.core.ObjectEntity;
import ge.comcom.anubis.entity.meta.PropertyDef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PropertyValueMultiRepository extends JpaRepository<PropertyValueMulti, Long> {

    List<PropertyValueMulti> findAllByPropertyValueId(Long propertyValueId);

    @Transactional
    @Modifying
    @Query("delete from PropertyValueMulti m where m.propertyValue.id = :propertyValueId")
    void deleteAllByPropertyValueId(Long propertyValueId);

    @Query("""
        SELECT pvm
        FROM PropertyValueMulti pvm
        WHERE pvm.propertyValue.objectVersion.object = :object
          AND pvm.propertyValue.propertyDef = :propertyDef
    """)
    List<PropertyValueMulti> findByObjectAndPropertyDef(ObjectEntity object,
                                                        PropertyDef propertyDef);
}
