package ge.comcom.anubis.dto.mapper;

import ge.comcom.anubis.dto.core.ObjectVersionDto;
import ge.comcom.anubis.entity.core.ObjectVersionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Маппер для преобразования между ObjectVersionEntity и ObjectVersionDto.
 * Содержит кастомное преобразование Instant ↔ LocalDateTime.
 */
@Mapper(componentModel = "spring")
public interface ObjectVersionMapper {

    // === Entity → DTO ===
    @Mapping(source = "createdAt", target = "createdAt", qualifiedByName = "instantToLocalDateTime")
    ObjectVersionDto toDto(ObjectVersionEntity entity);

    // === DTO → Entity ===
    @Mapping(source = "createdAt", target = "createdAt", qualifiedByName = "localDateTimeToInstant")
    ObjectVersionEntity toEntity(ObjectVersionDto dto);

    // --- Кастомные преобразования времени ---

    @Named("instantToLocalDateTime")
    default LocalDateTime instantToLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    @Named("localDateTimeToInstant")
    default Instant localDateTimeToInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }
}
