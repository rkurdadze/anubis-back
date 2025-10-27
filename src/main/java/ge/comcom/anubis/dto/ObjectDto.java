package ge.comcom.anubis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Data Transfer Object representing an object entity.
 */
// ObjectDto.java
@Data
@Schema(description = "Data structure for repository objects")
public class ObjectDto {

    @Schema(description = "Unique identifier", example = "101")
    private Long id;

    @Schema(description = "Human-readable name", example = "Network License Agreement")
    @NotBlank
    private String name;

    @Schema(description = "Object type ID", example = "3")
    @NotNull
    @Positive
    private Long typeId;

    @Schema(description = "Object class ID (optional)", example = "1")
    @Positive
    private Long classId;

    @Schema(description = "Vault ID", example = "2")
    @Positive
    private Long vaultId;

    @Schema(description = "Soft-deleted flag", example = "false")
    private Boolean isDeleted;

    @Schema(description = "Creation timestamp (from first version)", example = "2025-10-24T14:55:00")
    private LocalDateTime createdAt;

    @Schema(description = "Creator username (from first version)", example = "admin")
    private String createdBy;
}
