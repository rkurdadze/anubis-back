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

    private Long id;
    private Long valueListId;
    private String value; // ✅ основное текстовое значение
    private Map<String, String> valueI18n; // ✅ jsonb
    private Integer sortOrder;
    private Boolean isActive;
    private Long parentItemId;
    private String externalCode;
}
