package ge.comcom.anubis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Запрос на создание/обновление логического vault.
 */
@Data
public class VaultRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String name;

    private String description;

    private Long defaultStorageId;

    @JsonProperty("isActive")
    private boolean active;
}
