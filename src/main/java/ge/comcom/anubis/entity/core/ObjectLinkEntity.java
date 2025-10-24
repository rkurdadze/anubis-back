package ge.comcom.anubis.entity.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ge.comcom.anubis.entity.security.User;
import ge.comcom.anubis.enums.LinkDirection;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.Instant;

/**
 * Represents a relationship between two objects (like M-Files Object Links).
 * Supports unidirectional (UNI) and bidirectional (BI) connections.
 */
@Entity
@Table(
        name = "object_link",
        uniqueConstraints = @UniqueConstraint(columnNames = {"src_object_id", "dst_object_id", "role_id"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ObjectLinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Unique identifier of the link")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "src_object_id", nullable = false)
    @Comment("Source object of the link")
    @JsonIgnoreProperties({"links", "versions"}) // защита от циклов
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ObjectEntity source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dst_object_id", nullable = false)
    @Comment("Target object of the link")
    @JsonIgnoreProperties({"links", "versions"})
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ObjectEntity target;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    @Comment("Relationship role reference. Example: 'Customer'.")
    @JsonIgnoreProperties({"sourceType", "targetType"})
    private LinkRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    @Comment("Direction of link: UNI or BI")
    private LinkDirection direction;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Comment("Timestamp when the link was created")
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    @Comment("User who created this link")
    @JsonIgnoreProperties({"roles", "permissions"})
    private User createdBy;
}
