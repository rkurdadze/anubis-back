package ge.comcom.anubis.repository.meta;

import ge.comcom.anubis.entity.core.ObjectClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClassRepository extends JpaRepository<ObjectClass, Long> {

    Optional<ObjectClass> findByObjectTypeIdAndNameIgnoreCase(Long objectTypeId, String name);

    boolean existsByObjectTypeIdAndNameIgnoreCase(Long objectTypeId, String name);

    boolean existsByObjectTypeIdAndNameIgnoreCaseAndIdNot(Long objectTypeId, String name, Long excludeId);

    Optional<ObjectClass> findByObjectTypeAndNameIgnoreCase(ge.comcom.anubis.entity.core.ObjectType objectType, String name);
}
