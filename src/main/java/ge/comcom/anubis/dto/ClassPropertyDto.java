package ge.comcom.anubis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO для связи Class ↔ PropertyDef")
public class ClassPropertyDto {
    private Long classId;
    private Long propertyDefId;
    private Boolean isReadonly;
    private Boolean isHidden;
    private Integer displayOrder;
    private Boolean isActive; // ✅ добавляем флаг активности

    @Schema(description = "Имя связанного свойства", example = "CustomerName")
    private String propertyName; // ✅ новое поле
}

