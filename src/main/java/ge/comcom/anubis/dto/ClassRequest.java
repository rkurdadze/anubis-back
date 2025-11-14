package ge.comcom.anubis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Payload for creating or updating a metadata class.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for creating or updating class definitions")
public class ClassRequest {

    @NotNull(message = "objectTypeId is required")
    @Schema(description = "ObjectType ID to which this class belongs", example = "3")
    private Long objectTypeId;

    @Schema(description = "ACL ID linked with this class", example = "10")
    private Long aclId;

    @Schema(description = "Parent class ID", example = "12")
    private Long parentClassId;

    @NotBlank(message = "name is required")
    @Size(max = 255, message = "name must not exceed 255 characters")
    @Schema(description = "Class name", example = "Broadcast Station")
    private String name;

    @Size(max = 2000, message = "description must not exceed 2000 characters")
    @Schema(description = "Optional description", example = "Represents a radio broadcasting station")
    private String description;

    @Schema(description = "Active status (optional, defaults to true)", example = "true")
    private Boolean isActive;
}
