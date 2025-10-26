package ge.comcom.anubis.repository.meta;

import ge.comcom.anubis.entity.core.ValueList;
import ge.comcom.anubis.repository.BaseActiveRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing Value Lists (dictionaries / picklists).
 * Maps directly to table "value_list".
 *
 * Examples:
 *  - findByNameIgnoreCase("DocumentStatus")
 *  - existsByNameIgnoreCase("CustomerType")
 */
@Repository
public interface ValueListRepository extends BaseActiveRepository<ValueList, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long excludeId);
}

