package ge.comcom.anubis.mapper.meta;

import ge.comcom.anubis.dto.ValueListDto;
import ge.comcom.anubis.entity.core.ValueList;
import org.springframework.stereotype.Component;

@Component
public class ValueListMapper {

    public ValueListDto toDto(ValueList entity) {
        if (entity == null) {
            return null;
        }
        return ValueListDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .nameI18n(entity.getNameI18n())
                .isActive(entity.getIsActive())
                .build();
    }

    public ValueList toEntity(ValueListDto dto) {
        if (dto == null) {
            return null;
        }
        return ValueList.builder()
                .id(dto.getId())
                .name(trim(dto.getName()))
                .nameI18n(dto.getNameI18n())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : Boolean.TRUE)
                .build();
    }

    public void updateEntityFromDto(ValueListDto dto, ValueList entity) {
        if (dto == null || entity == null) {
            return;
        }
        if (dto.getName() != null && !dto.getName().isBlank()) {
            entity.setName(trim(dto.getName()));
        }
        if (dto.getNameI18n() != null) {
            entity.setNameI18n(dto.getNameI18n());
        }
        if (dto.getIsActive() != null) {
            entity.setIsActive(dto.getIsActive());
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
