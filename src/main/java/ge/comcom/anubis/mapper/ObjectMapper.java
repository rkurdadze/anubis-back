package ge.comcom.anubis.mapper;


import ge.comcom.anubis.dto.ObjectDto;
import ge.comcom.anubis.entity.core.ObjectEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Mapper for converting between ObjectEntity and ObjectDto.
 */
@Mapper(componentModel = "spring")
public interface ObjectMapper {

    @Mapping(source = "createdAt", target = "createdAt", qualifiedByName = "instantToLocalDateTime")
    ObjectDto toDto(ObjectEntity entity);

    @Mapping(source = "createdAt", target = "createdAt", qualifiedByName = "localDateTimeToInstant")
    ObjectEntity toEntity(ObjectDto dto);

    @Named("instantToLocalDateTime")
    default LocalDateTime instantToLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    @Named("localDateTimeToInstant")
    default Instant localDateTimeToInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }
}
