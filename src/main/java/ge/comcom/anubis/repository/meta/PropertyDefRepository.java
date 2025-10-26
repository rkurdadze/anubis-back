package ge.comcom.anubis.repository.meta;

import ge.comcom.anubis.entity.meta.PropertyDef;
import ge.comcom.anubis.repository.BaseActiveRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PropertyDefRepository extends BaseActiveRepository<PropertyDef, Long> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long excludeId);
}

