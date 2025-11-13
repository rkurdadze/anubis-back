package ge.comcom.anubis.mapper;

import ge.comcom.anubis.dto.ObjectFileDto;
import ge.comcom.anubis.entity.core.ObjectFileEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ObjectFileMapper {

    @Mapping(target = "objectId", source = "version.object.id")
    @Mapping(target = "versionId", source = "version.id")
    @Mapping(target = "filename", source = "fileName")
    @Mapping(target = "size", source = "binary.size")
    @Mapping(target = "mimeType", source = "binary.mimeType")
    ObjectFileDto toDto(ObjectFileEntity entity);

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "binary", ignore = true)
    @Mapping(target = "fileName", source = "filename")
    @Mapping(target = "deleted", ignore = true)
    ObjectFileEntity toEntity(ObjectFileDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "binary", ignore = true)
    @Mapping(target = "fileName", source = "filename")
    @Mapping(target = "deleted", ignore = true)
    void updateEntityFromDto(ObjectFileDto dto, @MappingTarget ObjectFileEntity entity);
}
