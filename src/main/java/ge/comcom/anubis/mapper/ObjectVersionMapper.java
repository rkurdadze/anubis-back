package ge.comcom.anubis.mapper;

import ge.comcom.anubis.dto.ObjectVersionDto;
import ge.comcom.anubis.entity.core.ObjectVersionEntity;
import org.mapstruct.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Mapper(componentModel = "spring")
public interface ObjectVersionMapper {

    // === Конвертеры (default методы) ===

    @Named("safeUsername")
    default String safeUsername(ge.comcom.anubis.entity.security.User user) {
        return user != null ? user.getUsername() : null;
    }

    @Named("instantToLocalDateTime")
    default LocalDateTime instantToLocalDateTime(Instant instant) {
        return instant == null ? null : instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    @Named("localDateTimeToInstant")
    default Instant localDateTimeToInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    // === Маппинги ===

    @Mapping(target = "objectId", source = "object.id")
    @Mapping(target = "versionNum", source = "versionNumber")
    @Mapping(target = "createdByName", source = "createdBy", qualifiedByName = "safeUsername")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "instantToLocalDateTime")
    @Mapping(target = "modifiedAt", source = "modifiedAt", qualifiedByName = "instantToLocalDateTime")
    ObjectVersionDto toDto(ObjectVersionEntity entity);

    @Mapping(target = "object", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "files", ignore = true)
    @Mapping(target = "acl", ignore = true)
    @Mapping(target = "lockedBy", ignore = true)
    @Mapping(target = "versionNumber", source = "versionNum")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "localDateTimeToInstant")
    @Mapping(target = "modifiedAt", source = "modifiedAt", qualifiedByName = "localDateTimeToInstant")
    ObjectVersionEntity toEntity(ObjectVersionDto dto);
}