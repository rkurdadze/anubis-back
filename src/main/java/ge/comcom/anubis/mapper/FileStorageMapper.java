package ge.comcom.anubis.mapper;

import ge.comcom.anubis.dto.FileStorageDto;
import ge.comcom.anubis.entity.core.FileStorageEntity;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FileStorageMapper {

    FileStorageDto toDto(FileStorageEntity entity);

    @InheritInverseConfiguration
    FileStorageEntity toEntity(FileStorageDto dto);
}
