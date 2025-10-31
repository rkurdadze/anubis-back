package ge.comcom.anubis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * DTO для описания типа объекта (ObjectType).
 */
@Data
@Schema(description = "Data structure representing an object type definition")
public class ObjectTypeDto {

    @Schema(description = "Unique identifier of the object type", example = "1")
    private Long id;

    @Schema(description = "Human-readable name of the object type", example = "Document")
    @NotBlank
    private String name;

    @Schema(description = "Vault ID owning this type", example = "2")
    @NotNull
    @Positive
    private Long vaultId;

    @Schema(description = "Vault name (read-only)", example = "Main Vault", accessMode = Schema.AccessMode.READ_ONLY)
    private String vaultName;
}
