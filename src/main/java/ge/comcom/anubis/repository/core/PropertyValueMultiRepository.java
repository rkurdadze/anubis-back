package ge.comcom.anubis.repository.core;

import ge.comcom.anubis.entity.core.PropertyValueMulti;
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
}

