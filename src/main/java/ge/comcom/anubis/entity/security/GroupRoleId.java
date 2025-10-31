package ge.comcom.anubis.entity.security;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class GroupRoleId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "role_id")
    private Long roleId;
}
