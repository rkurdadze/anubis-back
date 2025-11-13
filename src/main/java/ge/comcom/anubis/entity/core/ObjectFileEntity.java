package ge.comcom.anubis.entity.core;

import jakarta.persistence.*;
import lombok.*;

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

    /** FK → file_storage.storage_id (defines storage type & config) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_id")
    private FileStorageEntity storage;

    /** Soft-delete flag: TRUE → file is logically deleted */
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "binary_id", nullable = false)
    private FileBinaryEntity binary;

}
