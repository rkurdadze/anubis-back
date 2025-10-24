package ge.comcom.anubis.repository.meta;

import ge.comcom.anubis.entity.core.ValueListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ValueListItemRepository extends JpaRepository<ValueListItem, Long> {

    List<ValueListItem> findAllByValueListIdOrderBySortOrderAsc(Long valueListId);

    boolean existsByValueListIdAndValueTextIgnoreCase(Long valueListId, String valueText);

    boolean existsByValueListIdAndValueTextIgnoreCaseAndIdNot(Long valueListId, String valueText, Long excludeId);
}

