package ge.comcom.anubis.entity.core;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Represents a single file attached to a specific object version.
 * Compatible with schema from V1__anubis_baseline.sql (table "object_file").
 *
 * <p>Supports multiple storage types:
 * <ul>
 *   <li>Inline (BYTEA) content in DB — {@code inline = true}</li>
 *   <li>External file path (Filesystem / S3) — {@code inline = false}</li>
 * </ul>
 */
@Entity
@Table(name = "object_file")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObjectFileEntity {

    /** Primary key (file_id) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long id;

    /** FK → object_version.version_id */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "object_version_id", nullable = false)
    private ObjectVersionEntity version;

    /** Original filename, e.g. "contract_v3.pdf" */
    @Column(name = "file_name", nullable = false)
    private String fileName;

    /** Binary file content, if stored inline in DB */
    @Lob
    @Column(name = "file_data")
    private byte[] content;

    /** External path or object key for FS/S3 storages */
    @Column(name = "external_file_path")
    private String externalFilePath;

    /** FK → file_storage.storage_id (defines storage type & config) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_id")
    private FileStorageEntity storage;

    /** TRUE → stored inline in DB, FALSE → in FS/S3 */
    @Builder.Default
    @Column(name = "inline")
    private boolean inline = true;

    /** File size in bytes */
    @Column(name = "file_size")
    private Long fileSize;

    /** MIME type, e.g. "application/pdf" */
    @Column(name = "content_type")
    private String mimeType;

}
