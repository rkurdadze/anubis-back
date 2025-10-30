package ge.comcom.anubis.mapper;

import ge.comcom.anubis.dto.PropertyDefDto;
import ge.comcom.anubis.dto.PropertyDefRequest;
import ge.comcom.anubis.entity.meta.PropertyDef;
import ge.comcom.anubis.enums.PropertyDataType;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PropertyDefMapper {

    @Mapping(target = "refObjectTypeId", source = "refObjectType.id")
    @Mapping(target = "valueListId", source = "valueList.id")
    PropertyDefDto toDto(PropertyDef entity);

    @Mapping(target = "name", expression = "java(trim(req.getName()))")
    @Mapping(target = "dataType", source = "dataType", qualifiedByName = "stringToPropertyDataType")
    @Mapping(target = "refObjectType", ignore = true)
    @Mapping(target = "valueList", ignore = true)
    @Mapping(target = "isMultiselect", expression = "java(defaultBoolean(req.getIsMultiselect()))")
    @Mapping(target = "isRequired", expression = "java(defaultBoolean(req.getIsRequired()))")
    @Mapping(target = "isUnique", expression = "java(defaultBoolean(req.getIsUnique()))")
    @Mapping(target = "isActive", constant = "true")
    PropertyDef toEntity(PropertyDefRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "name", ignore = true)
    @Mapping(target = "refObjectType", ignore = true)
    @Mapping(target = "valueList", ignore = true)
    @Mapping(target = "dataType", source = "dataType", qualifiedByName = "stringToPropertyDataType")
    void updateEntityFromRequest(PropertyDefRequest req, @MappingTarget PropertyDef entity);

    @Named("stringToPropertyDataType")
    default PropertyDataType mapDataType(String dataType) {
        if (dataType == null) {
            return null;
        }
        String normalized = dataType.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return PropertyDataType.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid property data type: " + dataType, ex);
        }
    }

    default Boolean defaultBoolean(Boolean value) {
        return value != null ? value : Boolean.FALSE;
    }

    default String trim(String value) {
        return value != null ? value.trim() : null;
    }
}
