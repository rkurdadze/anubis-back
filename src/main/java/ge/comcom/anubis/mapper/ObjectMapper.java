package ge.comcom.anubis.mapper;

import ge.comcom.anubis.dto.ObjectDto;
import ge.comcom.anubis.entity.core.ObjectEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Mapper(componentModel = "spring")
public interface ObjectMapper {

    // Убираем mapping для createdAt
    ObjectDto toDto(ObjectEntity entity);

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
