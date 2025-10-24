package ge.comcom.anubis.repository.meta;

import ge.comcom.anubis.entity.meta.PropertyDef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PropertyDefRepository extends JpaRepository<PropertyDef, Long> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long excludeId);
}

