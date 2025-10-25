package ge.comcom.anubis.dto;

import ge.comcom.anubis.enums.PropertyDataType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for creating or updating property definitions")
public class PropertyDefRequest {

    @NotBlank
    @Schema(description = "Technical name", example = "Status")
    private String name;

    @Schema(description = "Localized captions JSON", example = "{\"en\":\"Status\",\"ru\":\"Статус\"}")
    private String captionI18n;

    @NotNull
    @Schema(description = "Data type (TEXT, NUMBER, DATE, BOOLEAN, LOOKUP, VALUELIST)", example = "VALUELIST")
    private PropertyDataType dataType;

    @Schema(description = "FK to object_type for LOOKUP type", example = "2")
    private Long refObjectTypeId;

    @Schema(description = "FK to value_list for VALUELIST type", example = "10")
    private Long valueListId;

    @Schema(description = "Allows multiple values", example = "false")
    private Boolean isMultiselect;

    @Schema(description = "Is required", example = "true")
    private Boolean isRequired;

    @Schema(description = "Must be unique across class scope", example = "false")
    private Boolean isUnique;

    @Schema(description = "Validation regex", example = "^[A-Z0-9_-]{3,20}$")
    private String regex;

    @Schema(description = "Default value (as string)", example = "Draft")
    private String defaultValue;

    @Schema(description = "Description", example = "Approval status")
    private String description;
}
