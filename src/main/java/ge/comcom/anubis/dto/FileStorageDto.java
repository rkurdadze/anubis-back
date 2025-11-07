package ge.comcom.anubis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import ge.comcom.anubis.enums.StorageKindEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * DTO representation of a physical file storage configuration.
 */
@Data
@Schema(description = "Details of a file storage configuration exposed via the API.")
public class FileStorageDto {

    @Schema(description = "Unique identifier of the storage entry", example = "1")
    private Long id;
    @Schema(description = "Type of backend used for storing files", example = "DB")
    private StorageKindEnum kind;
    @Schema(description = "Display name of the storage", example = "Primary Database Storage")
    private String name;
    @Schema(description = "Optional description visible to administrators", example = "Stores binaries in PostgreSQL")
    private String description;
    @Schema(description = "Filesystem path configured for FS storage", example = "/data/anubis/files")
    private String basePath;
    @Schema(description = "Bucket name for S3-compatible storage", example = "anubis-files")
    private String bucket;
    @Schema(description = "Endpoint URL for the S3-compatible backend", example = "https://minio.local:9000")
    private String endpoint;
    @Schema(description = "Access key for S3-compatible storage", example = "minioadmin")
    private String accessKey;
    @Schema(description = "Secret key for S3-compatible storage", example = "minioadmin123")
    private String secretKey;

    @JsonProperty("isDefault")
    @Schema(description = "Indicates whether this storage is the system-wide default", example = "true")
    private boolean defaultStorage;

    @JsonProperty("isActive")
    @Schema(description = "Shows whether the storage configuration is active", example = "true")
    private Boolean active;
}
