package ge.comcom.anubis.dto;

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

    private Boolean isActive = Boolean.TRUE;
}
