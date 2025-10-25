package ge.comcom.anubis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * DTO returned by API for metadata class.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Metadata Class DTO")
public class ClassDto {

    @Schema(description = "Unique identifier", example = "42")
    private Long id;

    @Schema(description = "Linked ObjectType ID", example = "3")
    private Long objectTypeId;

    @Schema(description = "ACL ID if any", example = "15")
    private Long aclId;

    @Schema(description = "Name of the class", example = "Broadcast Station")
    private String name;

    @Schema(description = "Optional description", example = "Represents radio broadcasting entities")
    private String description;

    @Schema(description = "Active flag", example = "true")
    private Boolean isActive;
}
