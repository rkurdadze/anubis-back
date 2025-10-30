package ge.comcom.anubis.mapper.meta;

import ge.comcom.anubis.dto.ValueListItemDto;
import ge.comcom.anubis.entity.core.ValueListItem;
import org.springframework.stereotype.Component;

@Component
public class ValueListItemMapper {

    public ValueListItemDto toDto(ValueListItem entity) {
        if (entity == null) {
            return null;
        }
        return ValueListItemDto.builder()
                .id(entity.getId())
                .valueListId(entity.getValueList() != null ? entity.getValueList().getId() : null)
                .value(entity.getValue())
                .valueI18n(entity.getValueI18n())
                .sortOrder(entity.getSortOrder())
                .isActive(entity.getIsActive())
                .parentItemId(entity.getParentItem() != null ? entity.getParentItem().getId() : null)
                .externalCode(entity.getExternalCode())
                .build();
    }

    public ValueListItem toEntity(ValueListItemDto dto) {
        if (dto == null) {
            return null;
        }
        return ValueListItem.builder()
                .id(dto.getId())
                .value(trim(dto.getValue()))
                .valueI18n(dto.getValueI18n())
                .sortOrder(dto.getSortOrder())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : Boolean.TRUE)
                .externalCode(dto.getExternalCode())
                .build();
    }

    public void updateEntityFromDto(ValueListItemDto dto, ValueListItem entity) {
        if (dto == null || entity == null) {
            return;
        }
        if (dto.getValue() != null && !dto.getValue().isBlank()) {
            entity.setValue(trim(dto.getValue()));
        }
        if (dto.getValueI18n() != null) {
            entity.setValueI18n(dto.getValueI18n());
        }
        if (dto.getSortOrder() != null) {
            entity.setSortOrder(dto.getSortOrder());
        }
        if (dto.getIsActive() != null) {
            entity.setIsActive(dto.getIsActive());
        }
        if (dto.getExternalCode() != null) {
            entity.setExternalCode(dto.getExternalCode());
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
