package ge.comcom.anubis.mapper.security;

import ge.comcom.anubis.dto.GroupDto;
import ge.comcom.anubis.entity.security.Group;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class GroupMapper {

    @Autowired
    private SecurityMappingHelper securityMappingHelper;

    @Mapping(target = "memberIds", ignore = true)
    @Mapping(target = "roleIds", ignore = true)
    public abstract GroupDto toDto(Group entity);

    @AfterMapping
    protected void fillMembers(Group entity, @MappingTarget GroupDto dto) {
        dto.setMemberIds(securityMappingHelper.loadMemberIdsForGroup(entity.getId()));
        dto.setRoleIds(securityMappingHelper.loadRoleIdsForGroup(entity.getId()));
    }
}
