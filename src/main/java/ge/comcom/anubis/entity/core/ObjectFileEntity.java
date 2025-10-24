package ge.comcom.anubis.entity.core;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.Instant;

/**
 * Represents a single file attached to an object version.
 */
@Entity
@Table(name = "object_file")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObjectFileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Unique identifier of the file record")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    @Comment("Parent version reference")
    private ObjectVersionEntity version;

    @Column(name = "file_name", nullable = false)
    @Comment("Original file name")
    private String fileName;

    @Column(name = "mime_type", nullable = false)
    @Comment("MIME type of the file (e.g. application/pdf)")
    private String mimeType;

    @Column(name = "file_size")
    @Comment("Size of the file in bytes")
    private Long fileSize;

    @Lob
    @Column(name = "content", nullable = false)
    @Comment("Binary file content stored in DB as BLOB")
    private byte[] content;

    @Column(name = "uploaded_by", nullable = false)
    @Comment("User who uploaded the file")
    private String uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    @Comment("Timestamp when file was uploaded")
    private Instant uploadedAt;
}
