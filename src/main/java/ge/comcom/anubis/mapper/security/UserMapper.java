package ge.comcom.anubis.mapper.security;

import ge.comcom.anubis.dto.UserDto;
import ge.comcom.anubis.entity.security.User;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class UserMapper {

    @Autowired
    private SecurityMappingHelper securityMappingHelper;

    @Mapping(target = "groupIds", ignore = true)
    @Mapping(target = "roleIds", ignore = true)
    public abstract UserDto toDto(User entity);

    @AfterMapping
    protected void fillRelations(User entity, @MappingTarget UserDto dto) {
        dto.setGroupIds(securityMappingHelper.loadGroupIdsForUser(entity.getId()));
        dto.setRoleIds(securityMappingHelper.loadRoleIdsForUser(entity.getId()));
    }
}
