package ge.comcom.anubis.repository.meta;

import ge.comcom.anubis.entity.core.ObjectType;
import ge.comcom.anubis.entity.meta.PropertyDef;
import ge.comcom.anubis.repository.BaseActiveRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PropertyDefRepository extends BaseActiveRepository<PropertyDef, Long> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long excludeId);

    Optional<PropertyDef> findByRefObjectTypeAndNameIgnoreCase(ObjectType objectType, String name);

    Optional<PropertyDef> findByNameIgnoreCase(String name);

    @Query("""
        SELECT p FROM PropertyDef p
        JOIN ClassProperty cp ON cp.propertyDef.id = p.id
        WHERE cp.objectClass.id = :classId
          AND LOWER(p.name) = LOWER(:name)
    """)
    Optional<PropertyDef> findByClassIdAndNameIgnoreCase(@Param("classId") Long classId,
                                                         @Param("name") String name);
}