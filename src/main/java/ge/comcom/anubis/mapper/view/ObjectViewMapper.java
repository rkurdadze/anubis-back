package ge.comcom.anubis.mapper.view;

import ge.comcom.anubis.dto.ObjectViewDto;
import ge.comcom.anubis.dto.ViewGroupingDto;
import ge.comcom.anubis.entity.view.ObjectViewEntity;
import ge.comcom.anubis.entity.view.ObjectViewGroupingEntity;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = JsonMapperUtils.class)
public interface ObjectViewMapper {

    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "filterJson", source = "filterJson", qualifiedBy = JsonMappingQualifier.class)
    ObjectViewDto toDto(ObjectViewEntity entity);

    @InheritInverseConfiguration
    @Mapping(target = "filterJson", source = "filterJson", qualifiedBy = JsonMappingQualifier.class)
    ObjectViewEntity toEntity(ObjectViewDto dto);

    List<ObjectViewDto> toDtoList(List<ObjectViewEntity> entities);
    List<ObjectViewEntity> toEntityList(List<ObjectViewDto> dtos);

    @Mapping(target = "propertyDefId", source = "propertyDef.id")
    ViewGroupingDto toDto(ObjectViewGroupingEntity entity);

    @InheritInverseConfiguration
    ObjectViewGroupingEntity toEntity(ViewGroupingDto dto);
}
