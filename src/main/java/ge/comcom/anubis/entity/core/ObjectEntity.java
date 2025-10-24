package ge.comcom.anubis.entity.core;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a logical object in the repository.
 * Each object can have multiple versions and attached files.
 */
@Entity
@Table(name = "object")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Unique identifier of the object")
    private Long id;

    @Column(nullable = false)
    @Comment("Human-readable name of the object")
    private String name;

    @Column(name = "type_id", nullable = false)
    @Comment("Reference to object type (foreign key to object_type table)")
    private Long typeId;

    @Column(name = "created_by", nullable = false)
    @Comment("Username or user ID who created the object")
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    @Comment("Timestamp when object was created")
    private Instant createdAt;

    @Column(name = "description")
    @Comment("Optional description or notes about the object")
    private String description;

    @Column(name = "is_archived", nullable = false)
    @Comment("Indicates whether the object is archived")
    private Boolean isArchived = false;

    @OneToMany(mappedBy = "object", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Comment("List of all versions belonging to this object")
    private List<ObjectVersionEntity> versions = new ArrayList<>();
}