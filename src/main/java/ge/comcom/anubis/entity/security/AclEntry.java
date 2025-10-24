package ge.comcom.anubis.entity.security;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "acl_entry")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AclEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "entry_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acl_id", nullable = false)
    private Acl acl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @Column(name = "can_read")
    private Boolean canRead = false;

    @Column(name = "can_write")
    private Boolean canWrite = false;

    @Column(name = "can_delete")
    private Boolean canDelete = false;

    @Column(name = "can_change_permissions")
    private Boolean canChangePermissions = false;
}
