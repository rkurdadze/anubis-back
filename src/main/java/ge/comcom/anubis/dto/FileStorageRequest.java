package ge.comcom.anubis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import ge.comcom.anubis.enums.StorageKindEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Запрос на создание/обновление конфигурации хранилища файлов.
 */
@Data
public class FileStorageRequest {

    @NotNull
    private StorageKindEnum kind;

    @NotBlank
    private String name;

    private String description;

    private String basePath;
    private String bucket;
    private String endpoint;
    private String accessKey;
    private String secretKey;

    @JsonProperty("isDefault")
    private boolean defaultStorage;

    @JsonProperty("isActive")
    private boolean active;
}
