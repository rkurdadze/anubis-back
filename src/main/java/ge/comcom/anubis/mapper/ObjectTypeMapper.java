package ge.comcom.anubis.mapper;

import ge.comcom.anubis.dto.ObjectTypeDto;
import ge.comcom.anubis.entity.core.ObjectType;
import ge.comcom.anubis.entity.core.VaultEntity;
import ge.comcom.anubis.entity.security.Acl;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ObjectTypeMapper {

    // ===============================
    // Entity -> DTO
    // ===============================
    @Mapping(source = "vault.id", target = "vaultId")
    @Mapping(source = "vault.name", target = "vaultName")
    @Mapping(source = "vault.active", target = "vaultActive")
    @Mapping(source = "acl.id", target = "aclId")
    @Mapping(source = "acl.name", target = "aclName")
    ObjectTypeDto toDto(ObjectType entity);

    // ===============================
    // DTO -> Entity
    // ===============================
    @InheritInverseConfiguration
    @Mapping(target = "vault", source = "vaultId", qualifiedByName = "mapVaultFromId")
    @Mapping(target = "acl", source = "aclId", qualifiedByName = "mapAclFromId")
    ObjectType toEntity(ObjectTypeDto dto);

    // ===============================
    // Helper: создаёт VaultEntity по ID
    // ===============================
    @Named("mapVaultFromId")
    default VaultEntity mapVaultFromId(Long vaultId) {
        if (vaultId == null) {
            return null;
        }
        return VaultEntity.builder()
                .id(vaultId)
                .build();
    }

    @Named("mapAclFromId")
    default Acl mapAclFromId(Long aclId) {
        if (aclId == null) {
            return null;
        }
        return Acl.builder()
                .id(aclId)
                .build();
    }
}
