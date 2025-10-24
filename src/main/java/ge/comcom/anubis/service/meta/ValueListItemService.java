package ge.comcom.anubis.service.meta;

import ge.comcom.anubis.dto.meta.ValueListItemDto;
import ge.comcom.anubis.entity.core.ValueList;
import ge.comcom.anubis.entity.core.ValueListItem;
import ge.comcom.anubis.repository.meta.ValueListItemRepository;
import ge.comcom.anubis.repository.meta.ValueListRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing ValueListItem entities.
 * Handles CRUD operations with validation and mapping to DTOs.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ValueListItemService {

    private final ValueListRepository listRepository;
    private final ValueListItemRepository itemRepository;

    /**
     * Creates a new ValueListItem entry.
     */
    public ValueListItemDto create(ValueListItemDto req) {
        ValueList list = listRepository.findById(req.getValueListId())
                .orElseThrow(() -> new EntityNotFoundException("ValueList not found"));

        // Prevent duplicates within the same list
        if (itemRepository.existsByValueListIdAndValueIgnoreCase(req.getValueListId(), req.getValue())) {
            throw new IllegalArgumentException("Value already exists in this list");
        }

        ValueListItem parent = null;
        if (req.getParentItemId() != null) {
            parent = itemRepository.findById(req.getParentItemId())
                    .orElseThrow(() -> new EntityNotFoundException("Parent item not found"));
        }

        ValueListItem entity = ValueListItem.builder()
                .valueList(list)
                .value(req.getValue() != null ? req.getValue().trim() : null)
                .valueI18n(req.getValueI18n())
                .sortOrder(req.getSortOrder())
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .parentItem(parent)
                .externalCode(req.getExternalCode())
                .build();

        return toDto(itemRepository.save(entity));
    }

    /**
     * Returns all ValueListItems for a given list, ordered by sortOrder.
     */
    @Transactional(readOnly = true)
    public List<ValueListItemDto> listByListId(Long valueListId) {
        return itemRepository.findAllByValueListIdOrderBySortOrderAsc(valueListId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Returns a single ValueListItem by ID.
     */
    @Transactional(readOnly = true)
    public ValueListItemDto get(Long id) {
        return toDto(itemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ValueListItem not found: id=" + id)));
    }

    /**
     * Updates existing ValueListItem.
     */
    public ValueListItemDto update(Long id, ValueListItemDto req) {
        ValueListItem e = itemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ValueListItem not found: id=" + id));

        if (req.getValue() != null && !req.getValue().isBlank()) {
            String val = req.getValue().trim();
            if (itemRepository.existsByValueListIdAndValueIgnoreCaseAndIdNot(e.getValueList().getId(), val, id))
                throw new IllegalArgumentException("Duplicate value in list");
            e.setValue(val);
        }

        if (req.getValueI18n() != null) e.setValueI18n(req.getValueI18n());
        if (req.getSortOrder() != null) e.setSortOrder(req.getSortOrder());
        if (req.getIsActive() != null) e.setIsActive(req.getIsActive());
        if (req.getExternalCode() != null) e.setExternalCode(req.getExternalCode());

        if (req.getParentItemId() != null) {
            e.setParentItem(itemRepository.findById(req.getParentItemId())
                    .orElseThrow(() -> new EntityNotFoundException("Parent item not found")));
        }

        return toDto(itemRepository.save(e));
    }

    /**
     * Deletes ValueListItem by ID.
     */
    public void delete(Long id) {
        if (!itemRepository.existsById(id))
            throw new EntityNotFoundException("ValueListItem not found: id=" + id);
        itemRepository.deleteById(id);
    }

    /**
     * Converts entity to DTO.
     */
    private ValueListItemDto toDto(ValueListItem e) {
        return ValueListItemDto.builder()
                .id(e.getId())
                .valueListId(e.getValueList() != null ? e.getValueList().getId() : null)
                .value(e.getValue())
                .valueI18n(e.getValueI18n())
                .sortOrder(e.getSortOrder())
                .isActive(e.getIsActive())
                .parentItemId(e.getParentItem() != null ? e.getParentItem().getId() : null)
                .externalCode(e.getExternalCode())
                .build();
    }
}
