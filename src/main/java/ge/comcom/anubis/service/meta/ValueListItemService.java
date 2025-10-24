package ge.comcom.anubis.service.meta;

import ge.comcom.anubis.dto.meta.ValueListItemDto;
import ge.comcom.anubis.entity.core.ValueList;
import ge.comcom.anubis.entity.core.ValueListItem;
import ge.comcom.anubis.repository.meta.ValueListRepository;
import ge.comcom.anubis.repository.meta.ValueListItemRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ValueListItemService {

    private final ValueListRepository listRepository;
    private final ValueListItemRepository itemRepository;

    public ValueListItemDto create(ValueListItemDto req) {
        ValueList list = listRepository.findById(req.getValueListId())
                .orElseThrow(() -> new EntityNotFoundException("ValueList not found"));

        if (itemRepository.existsByValueListIdAndValueTextIgnoreCase(req.getValueListId(), req.getValueText())) {
            throw new IllegalArgumentException("Value already exists in this list");
        }

        ValueListItem parent = null;
        if (req.getParentItemId() != null)
            parent = itemRepository.findById(req.getParentItemId())
                    .orElseThrow(() -> new EntityNotFoundException("Parent item not found"));

        ValueListItem entity = ValueListItem.builder()
                .valueList(list)
                .valueText(req.getValueText().trim())
                .valueTextI18n(req.getValueTextI18n())
                .sortOrder(req.getSortOrder())
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .parentItem(parent)
                .externalCode(req.getExternalCode())
                .build();

        return toDto(itemRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<ValueListItemDto> listByListId(Long valueListId) {
        return itemRepository.findAllByValueListIdOrderBySortOrderAsc(valueListId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ValueListItemDto get(Long id) {
        return toDto(itemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ValueListItem not found: id=" + id)));
    }

    public ValueListItemDto update(Long id, ValueListItemDto req) {
        ValueListItem e = itemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ValueListItem not found: id=" + id));

        if (req.getValueText() != null && !req.getValueText().isBlank()) {
            String name = req.getValueText().trim();
            if (itemRepository.existsByValueListIdAndValueTextIgnoreCaseAndIdNot(e.getValueList().getId(), name, id))
                throw new IllegalArgumentException("Duplicate value in list");
            e.setValueText(name);
        }

        if (req.getValueTextI18n() != null) e.setValueTextI18n(req.getValueTextI18n());
        if (req.getSortOrder() != null) e.setSortOrder(req.getSortOrder());
        if (req.getIsActive() != null) e.setIsActive(req.getIsActive());
        if (req.getExternalCode() != null) e.setExternalCode(req.getExternalCode());

        if (req.getParentItemId() != null) {
            e.setParentItem(itemRepository.findById(req.getParentItemId())
                    .orElseThrow(() -> new EntityNotFoundException("Parent item not found")));
        }

        return toDto(itemRepository.save(e));
    }

    public void delete(Long id) {
        if (!itemRepository.existsById(id))
            throw new EntityNotFoundException("ValueListItem not found: id=" + id);
        itemRepository.deleteById(id);
    }

    private ValueListItemDto toDto(ValueListItem e) {
        return ValueListItemDto.builder()
                .id(e.getId())
                .valueListId(e.getValueList() != null ? e.getValueList().getId() : null)
                .valueText(e.getValueText())
                .valueTextI18n(e.getValueTextI18n())
                .sortOrder(e.getSortOrder())
                .isActive(e.getIsActive())
                .parentItemId(e.getParentItem() != null ? e.getParentItem().getId() : null)
                .externalCode(e.getExternalCode())
                .build();
    }
}

