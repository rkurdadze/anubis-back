package ge.comcom.anubis.entity.core;

import ge.comcom.anubis.enums.PropertyDataType;
import ge.comcom.anubis.enums.StorageKindEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Represents a physical file storage configuration.
 * <p>
 * Supports DB, filesystem, and S3-compatible backends.
 * Matches schema from baseline migration (table "file_storage").
 */
@Entity
@Table(name = "file_storage")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class FileStorageEntity {

    /** Primary key (storage_id) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "storage_id")
    private Long id;

    /** Storage type: DB / FS / S3 */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "kind", nullable = false)
    private StorageKindEnum kind;

    /** Display name, e.g. "Local FS Storage" */
    @Column(name = "name")
    private String name;

    /** Human-readable description */
    @Column(name = "description")
    private String description;

    /** Base path for FS storage (e.g. /data/files) */
    @Column(name = "base_path")
    private String basePath;

    /** Bucket name for S3 storage */
    @Column(name = "bucket")
    private String bucket;

    /** S3 endpoint (e.g. https://minio.local:9000) */
    @Column(name = "endpoint")
    private String endpoint;

    /** Access key for S3 storage */
    @Column(name = "access_key")
    private String accessKey;

    /** Secret key for S3 storage */
    @Column(name = "secret_key")
    private String secretKey;

    /** Whether this is the system-wide default storage */
    @Column(name = "is_default")
    private boolean defaultStorage = false;

    /** Whether this storage is currently active */
    @Column(name = "is_active")
    private boolean active = true;

}
