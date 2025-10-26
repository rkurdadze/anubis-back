package ge.comcom.anubis.dto;

import ge.comcom.anubis.enums.PropertyDataType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Property Definition DTO")
public class PropertyDefDto {

    private Long id;

    @Schema(description = "Уникальное системное имя свойства")
    private String name;

    @Schema(description = "Локализованные подписи caption_i18n")
    private Map<String, String> captionI18n;

    @Schema(description = "Тип данных свойства")
    private PropertyDataType dataType;

    private Long refObjectTypeId;
    private Long valueListId;

    private Boolean isMultiselect;
    private Boolean isRequired;
    private Boolean isUnique;

    private String regex;
    private String defaultValue;
    private String description;

    private Boolean isActive; // если используешь soft-delete
}
