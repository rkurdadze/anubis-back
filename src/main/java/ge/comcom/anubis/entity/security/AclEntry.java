package ge.comcom.anubis.entity.security;

import ge.comcom.anubis.enums.GranteeType;
import jakarta.persistence.*;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents a single ACL entry that defines access rights
 * for a user or group (grantee).
 */
@Entity
@Table(name = "acl_entry")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Access Control List entry (permission record for user or group)")
public class AclEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "acl_entry_id")
    @Schema(description = "Unique identifier of the ACL entry", example = "101")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "acl_id", nullable = false,
            foreignKey = @ForeignKey(name = "acl_entry_acl_id_fkey"))
    @Schema(description = "Parent ACL record that this entry belongs to")
    private Acl acl;

    @Enumerated(EnumType.STRING)
    @Column(name = "grantee_type", nullable = false)
    @Schema(description = "Type of grantee: USER or GROUP", example = "USER")
    private GranteeType granteeType;

    @Column(name = "grantee_id", nullable = false)
    @Schema(description = "ID of the user or group the permissions apply to", example = "42")
    private Long granteeId;

    @Builder.Default
    @Column(name = "can_read", nullable = false)
    @Schema(description = "Permission flag for read access", example = "true")
    private Boolean canRead = true;

    @Builder.Default
    @Column(name = "can_write", nullable = false)
    @Schema(description = "Permission flag for write access", example = "false")
    private Boolean canWrite = false;

    @Builder.Default
    @Column(name = "can_delete", nullable = false)
    @Schema(description = "Permission flag for delete access", example = "false")
    private Boolean canDelete = false;

    @Builder.Default
    @Column(name = "can_change_acl", nullable = false)
    @Schema(description = "Permission flag for changing ACL", example = "false")
    private Boolean canChangeAcl = false;
}
