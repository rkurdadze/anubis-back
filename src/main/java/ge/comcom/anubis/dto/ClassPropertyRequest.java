package ge.comcom.anubis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for creating or updating class-property bindings")
public class ClassPropertyRequest {

    @NotNull
    @Schema(description = "Linked class ID", example = "5")
    private Long classId;

    @NotNull
    @Schema(description = "Linked property definition ID", example = "50")
    private Long propertyDefId;

    @Schema(description = "Read-only flag", example = "false")
    private Boolean isReadonly;

    @Schema(description = "Hidden flag", example = "false")
    private Boolean isHidden;

    @Schema(description = "Display order", example = "10")
    private Integer displayOrder;
}

