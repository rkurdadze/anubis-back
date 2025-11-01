package ge.comcom.anubis.mapper;

import ge.comcom.anubis.dto.ObjectTypeDto;
import ge.comcom.anubis.entity.core.ObjectType;
import ge.comcom.anubis.entity.core.VaultEntity;
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
    ObjectTypeDto toDto(ObjectType entity);

    // ===============================
    // DTO -> Entity
    // ===============================
    @InheritInverseConfiguration
    @Mapping(target = "vault", source = "vaultId", qualifiedByName = "mapVaultFromId")
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
}
