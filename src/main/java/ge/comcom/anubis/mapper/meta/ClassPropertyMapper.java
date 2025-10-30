package ge.comcom.anubis.mapper.meta;

import ge.comcom.anubis.dto.ClassPropertyDto;
import ge.comcom.anubis.dto.ClassPropertyRequest;
import ge.comcom.anubis.entity.meta.ClassProperty;
import ge.comcom.anubis.entity.meta.ClassPropertyId;
import org.springframework.stereotype.Component;

@Component
public class ClassPropertyMapper {

    public ClassPropertyDto toDto(ClassProperty entity) {
        if (entity == null) {
            return null;
        }
        return ClassPropertyDto.builder()
                .classId(entity.getClassId())
                .propertyDefId(entity.getPropertyDefId())
                .isReadonly(entity.getIsReadonly())
                .isHidden(entity.getIsHidden())
                .displayOrder(entity.getDisplayOrder())
                .isActive(entity.getIsActive())
                .build();
    }

    public ClassProperty toEntity(ClassPropertyRequest request) {
        if (request == null) {
            return null;
        }
        return ClassProperty.builder()
                .id(new ClassPropertyId(request.getClassId(), request.getPropertyDefId()))
                .isReadonly(request.getIsReadonly() != null ? request.getIsReadonly() : Boolean.FALSE)
                .isHidden(request.getIsHidden() != null ? request.getIsHidden() : Boolean.FALSE)
                .displayOrder(request.getDisplayOrder())
                .build();
    }

    public void updateEntityFromRequest(ClassPropertyRequest request, ClassProperty entity) {
        if (request == null || entity == null) {
            return;
        }
        if (request.getIsReadonly() != null) {
            entity.setIsReadonly(request.getIsReadonly());
        }
        if (request.getIsHidden() != null) {
            entity.setIsHidden(request.getIsHidden());
        }
        if (request.getDisplayOrder() != null) {
            entity.setDisplayOrder(request.getDisplayOrder());
        }
    }
}
