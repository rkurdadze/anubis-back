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
 * Represents a single version of an object (M-Files style).
 * Each version may contain files, metadata, ACL, and lock info.
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
    @Column(name = "version_id")
    @Comment("Primary key: unique version identifier")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "object_id", nullable = false)
    @Comment("FK to parent object")
    private ObjectEntity object;

    @Column(name = "version_num", nullable = false)
    @Comment("Sequential version number within object")
    private Integer versionNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Comment("Creation timestamp")
    private Instant createdAt;

    @Column(name = "modified_at")
    @Comment("Last modification timestamp")
    private Instant modifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    @Comment("User who created this version")
    private User createdBy;

    @Column(name = "comment")
    @Comment("User-provided comment or change note")
    private String comment;

    @Builder.Default
    @Column(name = "single_file", nullable = false)
    @Comment("If TRUE, version expects a single file only")
    private Boolean singleFile = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acl_id")
    @Comment("Optional ACL overriding object/class/type ACL")
    private Acl acl;

    @Builder.Default
    @Column(name = "is_locked", nullable = false)
    @Comment("TRUE if version is currently locked (checked-out)")
    private Boolean isLocked = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locked_by")
    @Comment("User who locked the version")
    private User lockedBy;

    @Column(name = "locked_at")
    @Comment("Timestamp of lock creation")
    private Instant lockedAt;

    @Builder.Default
    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Comment("List of all attached files for this version")
    private List<ObjectFileEntity> files = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "objectVersion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Comment("Metadata values associated with this version")
    private List<PropertyValue> propertyValues = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (modifiedAt == null) modifiedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedAt = Instant.now();
    }
}
