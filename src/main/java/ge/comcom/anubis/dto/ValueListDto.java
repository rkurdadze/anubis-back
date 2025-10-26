package ge.comcom.anubis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO для справочника (ValueList)")
public class ValueListDto {
    private Long id;

    @Schema(description = "Имя справочника")
    private String name;

    @Schema(description = "Локализованные имена справочника (например, {\"en\": \"Status\", \"ru\": \"Статус\"})")
    private Map<String, String> nameI18n;

    @Schema(description = "Флаг активности (true = активен, false = деактивирован)")
    private Boolean isActive;
}
