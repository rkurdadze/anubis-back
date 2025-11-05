package ge.comcom.anubis.repository.meta;

import ge.comcom.anubis.entity.core.ValueList;
import ge.comcom.anubis.entity.core.ValueListItem;
import ge.comcom.anubis.repository.BaseActiveRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ValueListItemRepository extends BaseActiveRepository<ValueListItem, Long> {

    List<ValueListItem> findAllByValueListIdOrderBySortOrderAsc(Long valueListId);

    // ✅ Проверка дублей (новое имя поля — value)
    boolean existsByValueListIdAndValueIgnoreCase(Long valueListId, String value);

    // ✅ Проверка дублей при обновлении
    boolean existsByValueListIdAndValueIgnoreCaseAndIdNot(Long valueListId, String value, Long excludeId);


    Optional<ValueListItem> findByValueListAndValueIgnoreCase(ValueList valueList, String value);

    boolean existsByValueListAndValueIgnoreCase(ValueList valueList, String value);

}
