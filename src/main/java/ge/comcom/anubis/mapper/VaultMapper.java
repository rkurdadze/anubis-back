package ge.comcom.anubis.mapper;

import ge.comcom.anubis.dto.VaultDto;
import ge.comcom.anubis.entity.core.VaultEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = FileStorageMapper.class)
public interface VaultMapper {

    @Mapping(target = "defaultStorage", source = "defaultStorage")
    VaultDto toDto(VaultEntity entity);
}
