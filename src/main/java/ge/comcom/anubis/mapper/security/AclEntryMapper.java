package ge.comcom.anubis.mapper.security;

import ge.comcom.anubis.dto.AclEntryDto;
import ge.comcom.anubis.dto.AclEntryRequest;
import ge.comcom.anubis.entity.security.AclEntry;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface AclEntryMapper {

    @Mapping(target = "aclId", source = "acl.id")
    AclEntryDto toDto(AclEntry entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "acl", ignore = true)
    @Mapping(target = "canRead", expression = "java(defaultBoolean(request.getCanRead()))")
    @Mapping(target = "canWrite", expression = "java(defaultBoolean(request.getCanWrite()))")
    @Mapping(target = "canDelete", expression = "java(defaultBoolean(request.getCanDelete()))")
    @Mapping(target = "canChangeAcl", expression = "java(defaultBoolean(request.getCanChangeAcl()))")
    AclEntry toEntity(AclEntryRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "acl", ignore = true)
    void updateEntityFromRequest(AclEntryRequest request, @MappingTarget AclEntry entity);

    default Boolean defaultBoolean(Boolean value) {
        return value != null ? value : Boolean.FALSE;
    }
}
