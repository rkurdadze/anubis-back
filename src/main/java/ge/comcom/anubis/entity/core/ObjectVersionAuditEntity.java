package ge.comcom.anubis.entity.core;

import ge.comcom.anubis.enums.VersionChangeType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.Instant;

/**
 * Audit entity for tracking all version changes.
 */
@Entity
@Table(name = "object_version_audit")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObjectVersionAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    @Comment("Unique audit record ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    @Comment("Associated version")
    private ObjectVersionEntity version;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 50)
    @Comment("Type of change performed")
    private VersionChangeType changeType;

    @Column(name = "modified_by")
    @Comment("User ID who performed the change")
    private Long modifiedBy;

    @Column(name = "modified_at", nullable = false)
    @Comment("Timestamp when change was made")
    private Instant modifiedAt;

    @Column(name = "field_changed")
    @Comment("Name of the changed field, if applicable")
    private String fieldChanged;

    @Column(name = "old_value", columnDefinition = "TEXT")
    @Comment("Previous value of the field")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    @Comment("New value of the field")
    private String newValue;

    @Column(name = "change_summary", columnDefinition = "TEXT")
    @Comment("Human-readable description of the change")
    private String changeSummary;
}
