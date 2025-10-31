package ge.comcom.anubis.mapper;

import ge.comcom.anubis.dto.ObjectDto;
import ge.comcom.anubis.entity.core.ObjectClass;
import ge.comcom.anubis.entity.core.ObjectEntity;
import ge.comcom.anubis.entity.core.ObjectType;
import ge.comcom.anubis.entity.core.ObjectVersionEntity;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.util.List;

// ObjectMapper.java
@Mapper(componentModel = "spring", uses = {InstantMapper.class})
public interface ObjectMapper {

    @Mapping(target = "typeId", source = "objectType.id")
    @Mapping(target = "classId", source = "objectClass.id")
    @Mapping(target = "isDeleted", source = "isDeleted")
    @Mapping(target = "createdAt", source = "versions", qualifiedByName = "firstVersionCreatedAt")
    @Mapping(target = "createdBy", source = "versions", qualifiedByName = "firstVersionCreatedBy")
    ObjectDto toDto(ObjectEntity entity);

    @Mapping(target = "objectType", source = "typeId", qualifiedByName = "mapTypeId")
    @Mapping(target = "objectClass", source = "classId", qualifiedByName = "mapClassId")
    @Mapping(target = "versions", ignore = true)
    @Mapping(target = "outgoingLinks", ignore = true)
    @Mapping(target = "incomingLinks", ignore = true)
    @Mapping(target = "acl", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    ObjectEntity toEntity(ObjectDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "objectType", source = "typeId", qualifiedByName = "mapTypeId")
    @Mapping(target = "objectClass", source = "classId", qualifiedByName = "mapClassId")
    @Mapping(target = "versions", ignore = true)
    @Mapping(target = "outgoingLinks", ignore = true)
    @Mapping(target = "incomingLinks", ignore = true)
    @Mapping(target = "acl", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntityFromDto(ObjectDto dto, @MappingTarget ObjectEntity entity);

    @Named("firstVersionCreatedAt")
    default LocalDateTime firstVersionCreatedAt(List<ObjectVersionEntity> versions) {
        return versions == null || versions.isEmpty()
                ? null
                : InstantMapper.toLocalDateTime(versions.get(0).getCreatedAt());
    }

    @Named("firstVersionCreatedBy")
    default String firstVersionCreatedBy(List<ObjectVersionEntity> versions) {
        if (versions == null || versions.isEmpty()) {
            return null;
        }
        ObjectVersionEntity first = versions.get(0);
        if (first == null || first.getCreatedBy() == null) {
            return null;
        }
        return first.getCreatedBy().getUsername();
    }

    @Named("mapTypeId")
    default ObjectType mapTypeId(Long typeId) {
        if (typeId == null) {
            return null;
        }
        ObjectType type = new ObjectType();
        type.setId(typeId);
        return type;
    }

    @Named("mapClassId")
    default ObjectClass mapClassId(Long classId) {
        if (classId == null) {
            return null;
        }
        ObjectClass objectClass = new ObjectClass();
        objectClass.setId(classId);
        return objectClass;
    }
}
