package ge.comcom.anubis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Represents a specific version of an object including audit metadata.")
public class ObjectVersionDto {

    @Schema(description = "Unique identifier of the version", example = "42")
    private Long id;

    @Schema(description = "Identifier of the parent object", example = "7")
    private Long objectId;

    @Schema(description = "Sequential number of the version within the object", example = "3")
    private Integer versionNum;

    @Schema(description = "User-provided comment for the version", example = "Updated metadata")
    private String comment;

    @Schema(description = "Username of the version creator", example = "jdoe")
    private String createdByName;

    @Schema(description = "Creation timestamp in ISO-8601 format", example = "2025-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last modification timestamp in ISO-8601 format", example = "2025-01-16T08:45:00")
    private LocalDateTime modifiedAt;

    @Schema(description = "Indicates whether the version expects exactly one file", example = "true")
    private Boolean singleFile;
}
