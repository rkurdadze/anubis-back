package ge.comcom.anubis.entity.security;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "group_role")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupRole {

    @EmbeddedId
    private GroupRoleId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("groupId")
    @JoinColumn(name = "group_id", foreignKey = @ForeignKey(name = "group_role_group_id_fkey"))
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roleId")
    @JoinColumn(name = "role_id", foreignKey = @ForeignKey(name = "group_role_role_id_fkey"))
    private Role role;
}
