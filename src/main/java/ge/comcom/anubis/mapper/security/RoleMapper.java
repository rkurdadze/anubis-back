package ge.comcom.anubis.mapper.security;

import ge.comcom.anubis.dto.RoleDto;
import ge.comcom.anubis.dto.RoleRequest;
import ge.comcom.anubis.entity.security.Role;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    RoleDto toDto(Role entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "system", constant = "false")
    @Mapping(target = "active", expression = "java(request.getActive() == null ? Boolean.TRUE : request.getActive())")
    Role toEntity(RoleRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "system", ignore = true)
    void updateEntity(RoleRequest request, @MappingTarget Role entity);
}
