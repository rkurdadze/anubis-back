package ge.comcom.anubis.dto.mapper;


import ge.comcom.anubis.dto.core.ObjectFileDto;
import ge.comcom.anubis.entity.core.ObjectFileEntity;
import org.mapstruct.Mapper;

/**
 * Маппер для преобразования между ObjectFileEntity и ObjectFileDto.
 */
@Mapper(componentModel = "spring")
public interface ObjectFileMapper {
    ObjectFileDto toDto(ObjectFileEntity entity);
    ObjectFileEntity toEntity(ObjectFileDto dto);
}
