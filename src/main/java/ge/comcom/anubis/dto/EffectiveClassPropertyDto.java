package ge.comcom.anubis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Эффективное свойство класса с учётом наследования")
public class EffectiveClassPropertyDto {

    @Schema(description = "Идентификатор целевого класса", example = "101")
    private Long classId;

    @Schema(description = "Идентификатор PropertyDef", example = "55")
    private Long propertyDefId;

    @Schema(description = "Имя PropertyDef", example = "CustomerName")
    private String propertyName;

    @Schema(description = "Признак наследования", example = "true")
    private Boolean inherited;

    @Schema(description = "Класс, в котором определено свойство", example = "42")
    private Long sourceClassId;

    @Schema(description = "Имя класса-источника", example = "Документ")
    private String sourceClassName;

    @Schema(description = "Класс, который был переопределён", example = "10")
    private Long overriddenClassId;

    @Schema(description = "Признак переопределения свойств родителя", example = "false")
    private Boolean overridesParent;

    @Schema(description = "Флаг readonly", example = "false")
    private Boolean isReadonly;

    @Schema(description = "Флаг скрытия", example = "false")
    private Boolean isHidden;

    @Schema(description = "Порядок отображения", example = "20")
    private Integer displayOrder;
}
