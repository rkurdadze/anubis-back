package ge.comcom.anubis.entity.core;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "file_binary")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class FileBinaryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "binary_id")
    private Long id;

    @Column(name = "sha256")
    private String sha256;

    @Column(name = "inline", nullable = false)
    private boolean inline;

    @Lob
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "content")
    private byte[] content;

    @Column(name = "external_path")
    private String externalPath;

    @Column(name = "size")
    private Long size;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}