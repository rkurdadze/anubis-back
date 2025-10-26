package ge.comcom.anubis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO for ValueListItem")
public class ValueListItemDto {

    @Schema(description = "ID элемента списка значений")
    private Long id;

    @Schema(description = "ID родительского списка ValueList")
    private Long valueListId;

    @Schema(description = "Основное текстовое значение (value_text)")
    private String value;

    @Schema(description = "Локализованные подписи (value_text_i18n)")
    private Map<String, String> valueI18n;

    private Integer sortOrder;
    private Boolean isActive;
    private Long parentItemId;
    private String externalCode;
}
