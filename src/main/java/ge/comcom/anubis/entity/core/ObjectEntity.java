package ge.comcom.anubis.entity.core;

import ge.comcom.anubis.entity.security.Acl;
import ge.comcom.anubis.entity.security.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a logical object in the repository.
 * Matches PostgreSQL table: "object"
 */
@Entity
@Table(
        name = "\"object\"",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "object_object_type_id_class_id_name_key",
                        columnNames = {"object_type_id", "class_id", "name"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "object_id")
    @Comment("Primary key of the object")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "object_type_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "object_object_type_id_fkey"))
    @Comment("Reference to object type")
    private ObjectType objectType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id",
            foreignKey = @ForeignKey(name = "object_class_id_fkey"))
    @Comment("Reference to object class")
    private ObjectClass objectClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acl_id",
            foreignKey = @ForeignKey(name = "object_acl_id_fkey"))
    @Comment("Reference to ACL defining access permissions")
    private Acl acl;

    @Column(name = "name", nullable = false)
    @Comment("Human-readable name of the object")
    private String name;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    @Comment("Soft delete flag (TRUE means logically deleted)")
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    @Comment("Timestamp when the object was deleted (if applicable)")
    private Instant deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by",
            foreignKey = @ForeignKey(name = "object_deleted_by_fkey"))
    @Comment("User who performed deletion")
    private User deletedBy;

    @Builder.Default
    @OneToMany(mappedBy = "object", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Comment("All versions of this object")
    private List<ObjectVersionEntity> versions = new ArrayList<>();



    @Builder.Default
    @OneToMany(mappedBy = "source", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Comment("Outgoing object links (this object -> others)")
    private List<ObjectLinkEntity> outgoingLinks = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "target", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Comment("Incoming object links (others -> this object)")
    private List<ObjectLinkEntity> incomingLinks = new ArrayList<>();


    /** FK → vault.vault_id (defines which vault this object belongs to) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vault_id")
    private VaultEntity vault;
}
