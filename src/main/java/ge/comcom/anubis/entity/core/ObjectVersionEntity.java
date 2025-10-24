package ge.comcom.anubis.entity.core;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a version of an object.
 * Each version can have multiple attached files.
 */
@Entity
@Table(name = "object_version")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObjectVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Unique identifier of the version")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "object_id", nullable = false)
    @Comment("Parent object reference")
    private ObjectEntity object;

    @Column(name = "version_number", nullable = false)
    @Comment("Sequential number of this version")
    private Integer versionNumber;

    @Column(name = "comment")
    @Comment("User-provided comment for this version")
    private String comment;

    @Column(name = "created_by", nullable = false)
    @Comment("User who created this version")
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    @Comment("Timestamp of version creation")
    private Instant createdAt;

    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Comment("List of all files attached to this version")
    private List<ObjectFileEntity> files = new ArrayList<>();
}
