package ge.comcom.anubis.mapper.meta;

import ge.comcom.anubis.dto.ClassDto;
import ge.comcom.anubis.dto.ClassRequest;
import ge.comcom.anubis.entity.core.ObjectClass;
import org.springframework.stereotype.Component;

@Component
public class ObjectClassMapper {

    public ClassDto toDto(ObjectClass entity) {
        if (entity == null) {
            return null;
        }
        return ClassDto.builder()
                .id(entity.getId())
                .objectTypeId(entity.getObjectType() != null ? entity.getObjectType().getId() : null)
                .aclId(entity.getAcl() != null ? entity.getAcl().getId() : null)
                .name(entity.getName())
                .description(entity.getDescription())
                .isActive(entity.getIsActive())
                .build();
    }

    public ObjectClass toEntity(ClassRequest request) {
        if (request == null) {
            return null;
        }
        return ObjectClass.builder()
                .name(trim(request.getName()))
                .description(request.getDescription())
                .isActive(request.getIsActive() != null ? request.getIsActive() : Boolean.TRUE)
                .build();
    }

    public void updateEntityFromRequest(ClassRequest request, ObjectClass entity) {
        if (request == null || entity == null) {
            return;
        }
        if (request.getName() != null && !request.getName().isBlank()) {
            entity.setName(trim(request.getName()));
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
