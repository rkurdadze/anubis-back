package ge.comcom.anubis.entity.core;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "object_file")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ObjectFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "object_version_id", nullable = false)
    private ObjectVersion version;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Lob
    @Column(name = "file_data")
    private byte[] fileData;

    @Column(name = "file_size")
    private Integer fileSize;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "external_file_path")
    private String externalFilePath;
}
