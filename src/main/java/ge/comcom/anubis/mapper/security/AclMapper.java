package ge.comcom.anubis.mapper.security;

import ge.comcom.anubis.dto.AclDto;
import ge.comcom.anubis.dto.AclRequest;
import ge.comcom.anubis.entity.security.Acl;
import ge.comcom.anubis.entity.security.AclEntry;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = "spring", uses = AclEntryMapper.class)
public abstract class AclMapper {

    @Autowired
    private SecurityMappingHelper securityMappingHelper;

    @Autowired
    private AclEntryMapper aclEntryMapper;

    @Mapping(target = "entries", ignore = true)
    protected abstract AclDto toDtoInternal(Acl entity, @Context boolean includeEntries);

    public AclDto toDto(Acl entity) {
        return toDtoInternal(entity, false);
    }

    public AclDto toDtoWithEntries(Acl entity) {
        return toDtoInternal(entity, true);
    }

    @AfterMapping
    protected void fillEntries(Acl entity, @MappingTarget AclDto dto, @Context boolean includeEntries) {
        if (!includeEntries) {
            dto.setEntries(List.of());
            return;
        }
        List<AclEntry> entries = securityMappingHelper.loadAclEntries(entity.getId());
        dto.setEntries(entries.stream().map(aclEntryMapper::toDto).toList());
    }

    @Mapping(target = "id", ignore = true)
    public abstract Acl toEntity(AclRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract void updateEntityFromRequest(AclRequest request, @MappingTarget Acl entity);

    @AfterMapping
    protected void normalizeName(AclRequest request, @MappingTarget Acl entity) {
        if (request.getName() != null) {
            entity.setName(trim(request.getName()));
        }
    }

    protected String trim(String value) {
        return value != null ? value.trim() : null;
    }
}
