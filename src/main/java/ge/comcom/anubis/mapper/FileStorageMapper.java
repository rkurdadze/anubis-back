package ge.comcom.anubis.mapper;

import ge.comcom.anubis.dto.FileStorageDto;
import ge.comcom.anubis.entity.core.FileStorageEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FileStorageMapper {

    FileStorageDto toDto(FileStorageEntity entity);
}
