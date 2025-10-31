package ge.comcom.anubis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import ge.comcom.anubis.enums.StorageKindEnum;
import lombok.Data;

/**
 * DTO представление конфигурации физического хранилища файлов.
 */
@Data
public class FileStorageDto {

    private Long id;
    private StorageKindEnum kind;
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
    private Boolean active;
}
