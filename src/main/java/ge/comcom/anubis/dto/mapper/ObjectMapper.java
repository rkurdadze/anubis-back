package ge.comcom.anubis.dto.mapper;

import ge.comcom.anubis.dto.core.ObjectDto;
import ge.comcom.anubis.entity.core.ObjectEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ObjectMapper {

    @Mapping(target = "objectTypeId", source = "objectType.id")
    @Mapping(target = "objectClassId", source = "objectClass.id")
    @Mapping(target = "objectTypeName", source = "objectType.name")
    @Mapping(target = "objectClassName", source = "objectClass.name")
    ObjectDto toDto(ObjectEntity entity);

    @InheritInverseConfiguration
    @Mapping(target = "objectType", ignore = true)
    @Mapping(target = "objectClass", ignore = true)
    ObjectEntity toEntity(ObjectDto dto);
}
