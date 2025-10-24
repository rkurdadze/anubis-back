package ge.comcom.anubis.entity.security;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class UserGroupId implements Serializable {
    private Long userId;
    private Long groupId;
}
