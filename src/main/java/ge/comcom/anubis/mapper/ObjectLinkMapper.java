package ge.comcom.anubis.mapper;

import ge.comcom.anubis.dto.ObjectLinkDto;
import ge.comcom.anubis.entity.core.ObjectLinkEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ObjectLinkMapper {

    @Mapping(target = "sourceId", source = "source.id")
    @Mapping(target = "targetId", source = "target.id")
    @Mapping(target = "roleId", source = "role.id")
    @Mapping(target = "roleName", source = "role.name")
    ObjectLinkDto toDto(ObjectLinkEntity entity);

    List<ObjectLinkDto> toDto(List<ObjectLinkEntity> entities);
}
