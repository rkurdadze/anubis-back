package ge.comcom.anubis.mapper;

import ge.comcom.anubis.dto.ObjectFileDto;
import ge.comcom.anubis.entity.core.ObjectFileEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ObjectFileMapper {

    @Mapping(target = "objectId", source = "version.object.id")
    @Mapping(target = "versionId", source = "version.id")
    @Mapping(target = "filename", source = "fileName")
    @Mapping(target = "size", source = "fileSize")
    ObjectFileDto toDto(ObjectFileEntity entity);

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "content", ignore = true)
    @Mapping(target = "externalFilePath", ignore = true)
    @Mapping(target = "storage", ignore = true)
    @Mapping(target = "inline", ignore = true)
    @Mapping(target = "fileName", source = "filename")
    @Mapping(target = "fileSize", source = "size")
    ObjectFileEntity toEntity(ObjectFileDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "content", ignore = true)
    @Mapping(target = "externalFilePath", ignore = true)
    @Mapping(target = "storage", ignore = true)
    @Mapping(target = "inline", ignore = true)
    @Mapping(target = "fileName", source = "filename")
    @Mapping(target = "fileSize", source = "size")
    void updateEntityFromDto(ObjectFileDto dto, @MappingTarget ObjectFileEntity entity);
}
