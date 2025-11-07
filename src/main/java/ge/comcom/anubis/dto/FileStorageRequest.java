package ge.comcom.anubis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import ge.comcom.anubis.enums.StorageKindEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for creating or updating a file storage configuration.
 */
@Data
@Schema(description = "Request payload for creating or updating file storage configuration entries.")
public class FileStorageRequest {

    @NotNull
    @Schema(description = "Type of the storage backend", requiredMode = Schema.RequiredMode.REQUIRED, example = "DB")
    private StorageKindEnum kind;

    @NotBlank
    @Schema(description = "Human-readable name of the storage", example = "Primary Database Storage")
    private String name;

    @Schema(description = "Optional description displayed to administrators", example = "Stores binaries in the database")
    private String description;

    @Schema(description = "Filesystem path used when kind is FS", example = "/data/anubis/files")
    private String basePath;
    @Schema(description = "Bucket name used for S3 storage", example = "anubis-files")
    private String bucket;
    @Schema(description = "Endpoint URL for S3-compatible storage", example = "https://minio.local:9000")
    private String endpoint;
    @Schema(description = "Access key for S3-compatible storage", example = "minioadmin")
    private String accessKey;
    @Schema(description = "Secret key for S3-compatible storage", example = "minioadmin123")
    private String secretKey;

    @JsonProperty("isDefault")
    @Schema(description = "Indicates whether this storage should become the system-wide default", example = "true")
    private boolean defaultStorage;

    @JsonProperty("isActive")
    @Schema(description = "Marks the storage as active or disabled", example = "true")
    private boolean active;
}
