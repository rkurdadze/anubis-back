package ge.comcom.anubis.mapper.security;

import ge.comcom.anubis.dto.AclEntryDto;
import ge.comcom.anubis.dto.AclEntryRequest;
import ge.comcom.anubis.entity.security.AclEntry;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class AclEntryMapper {

    @Autowired
    private SecurityMappingHelper securityMappingHelper;

    @Mapping(target = "aclId", source = "acl.id")
    @Mapping(target = "principal", ignore = true)
    public abstract AclEntryDto toDto(AclEntry entity);

    @AfterMapping
    protected void fillPrincipal(AclEntry entity, @MappingTarget AclEntryDto dto) {
        dto.setPrincipal(securityMappingHelper.resolvePrincipal(entity.getGranteeType(), entity.getGranteeId()));
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "acl", ignore = true)
    @Mapping(target = "canRead", expression = "java(defaultBoolean(request.getCanRead()))")
    @Mapping(target = "canWrite", expression = "java(defaultBoolean(request.getCanWrite()))")
    @Mapping(target = "canDelete", expression = "java(defaultBoolean(request.getCanDelete()))")
    @Mapping(target = "canChangeAcl", expression = "java(defaultBoolean(request.getCanChangeAcl()))")
    public abstract AclEntry toEntity(AclEntryRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "acl", ignore = true)
    public abstract void updateEntityFromRequest(AclEntryRequest request, @MappingTarget AclEntry entity);

    protected Boolean defaultBoolean(Boolean value) {
        return value != null ? value : Boolean.FALSE;
    }
}
