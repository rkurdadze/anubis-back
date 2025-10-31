package ge.comcom.anubis.mapper.meta;

import ge.comcom.anubis.dto.ClassPropertyDto;
import ge.comcom.anubis.dto.ClassPropertyRequest;
import ge.comcom.anubis.entity.meta.ClassProperty;
import ge.comcom.anubis.entity.meta.ClassPropertyId;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ClassPropertyMapper {

    // --- Entity → DTO ---
    @Mapping(target = "classId", source = "id.classId")
    @Mapping(target = "propertyDefId", source = "id.propertyDefId")
    @Mapping(target = "propertyName", source = "propertyDef.name") // ✅ добавляем имя свойства
    ClassPropertyDto toDto(ClassProperty entity);

    // --- Request → Entity ---
    @Mapping(target = "id", expression = "java(new ClassPropertyId(request.getClassId(), request.getPropertyDefId()))")
    @Mapping(target = "objectClass", ignore = true)
    @Mapping(target = "propertyDef", ignore = true)
    @Mapping(target = "isReadonly", expression = "java(request.getIsReadonly() != null ? request.getIsReadonly() : Boolean.FALSE)")
    @Mapping(target = "isHidden", expression = "java(request.getIsHidden() != null ? request.getIsHidden() : Boolean.FALSE)")
    @Mapping(target = "isActive", constant = "true")
    ClassProperty toEntity(ClassPropertyRequest request);

    // --- Partial Update (PATCH) ---
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "objectClass", ignore = true)
    @Mapping(target = "propertyDef", ignore = true)
    @Mapping(target = "id", ignore = true)
    void updateEntityFromRequest(ClassPropertyRequest req, @MappingTarget ClassProperty entity);
}
