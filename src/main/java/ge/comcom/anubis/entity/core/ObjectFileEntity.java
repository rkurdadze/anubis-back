package ge.comcom.anubis.entity.core;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "object_file")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObjectFileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "object_id", nullable = false)
    private Long objectId;

    @Column(name = "version_id", nullable = false)
    private Long versionId;

    @Column(nullable = false)
    private String filename;

    @Column(name = "mime_type")
    private String mimeType;

    @Column
    private Long size;

    @Column(name = "uploaded_at")
    private Instant uploadedAt;

    @Lob
    @Column(nullable = false)
    private byte[] data;
}
