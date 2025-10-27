package ge.comcom.anubis.mapper;

import ge.comcom.anubis.dto.ObjectDto;
import ge.comcom.anubis.entity.core.ObjectEntity;
import ge.comcom.anubis.entity.core.ObjectVersionEntity;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.util.List;

// ObjectMapper.java
@Mapper(componentModel = "spring", uses = {InstantMapper.class})
public interface ObjectMapper {

    @Mapping(target = "typeId", source = "objectType.id")
    @Mapping(target = "classId", source = "objectClass.id")
    @Mapping(target = "vaultId", source = "vault.id")
    @Mapping(target = "isDeleted", source = "isDeleted")
    @Mapping(target = "createdAt", source = "versions", qualifiedByName = "firstVersionCreatedAt")
    @Mapping(target = "createdBy", source = "versions", qualifiedByName = "firstVersionCreatedBy")
    ObjectDto toDto(ObjectEntity entity);

    @Mapping(target = "objectType", ignore = true)
    @Mapping(target = "objectClass", ignore = true)
    @Mapping(target = "vault", ignore = true)
    @Mapping(target = "versions", ignore = true)
    @Mapping(target = "outgoingLinks", ignore = true)
    @Mapping(target = "incomingLinks", ignore = true)
    @Mapping(target = "acl", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    ObjectEntity toEntity(ObjectDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "objectType", ignore = true)
    @Mapping(target = "objectClass", ignore = true)
    @Mapping(target = "vault", ignore = true)
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
        return versions == null || versions.isEmpty()
                ? null
                : versions.get(0).getCreatedBy().getUsername();
    }
}