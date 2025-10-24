package ge.comcom.anubis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Data Transfer Object representing an object entity.
 */
@Data
@Schema(description = "Data structure for repository objects")
public class ObjectDto {

    @Schema(description = "Unique identifier of the object", example = "101")
    private Long id;

    @Schema(description = "Human-readable object name", example = "Network License Agreement")
    private String name;

    @Schema(description = "Type ID linked to object type table", example = "3")
    private Long typeId;

    @Schema(description = "User who created this object", example = "admin")
    private String createdBy;

    @Schema(description = "Creation timestamp (local time)", example = "2025-10-24T14:55:00")
    private LocalDateTime createdAt;

    @Schema(description = "Optional description or notes", example = "Initial object for telecom documentation")
    private String description;

    @Schema(description = "Indicates whether object is archived", example = "false")
    private Boolean isArchived;
}
