package ge.comcom.anubis.entity.security;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_group")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserGroup {

    @EmbeddedId
    private UserGroupId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("groupId")
    @JoinColumn(name = "group_id")
    private Group group;
}
