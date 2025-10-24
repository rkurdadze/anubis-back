package ge.comcom.anubis.entity.audit;

import ge.comcom.anubis.entity.core.ObjectVersion;
import ge.comcom.anubis.entity.security.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "object_version_audit")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ObjectVersionAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    private ObjectVersion version;

    @Column(name = "modified_at", nullable = false)
    private LocalDateTime modifiedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modified_by")
    private User modifiedBy;

    @Column(name = "change_type", nullable = false)
    private String changeType; // e.g. "property_change", "file_update"

    @Column(name = "field_changed")
    private String fieldChanged;

    @Column(name = "old_value")
    private String oldValue;

    @Column(name = "new_value")
    private String newValue;

    @Column(name = "change_summary")
    private String changeSummary;
}
