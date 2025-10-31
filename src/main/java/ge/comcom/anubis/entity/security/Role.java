package ge.comcom.anubis.entity.security;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

/**
 * Represents reusable security roles (similar to M-Files Named Roles).
 */
@Entity
@Table(name = "security_role")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    @Comment("Primary key")
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    @Comment("Role name, e.g. 'Full Control'")
    private String name;

    @Column(name = "description")
    @Comment("Optional description")
    private String description;

    @Builder.Default
    @Column(name = "is_system", nullable = false)
    @Comment("TRUE if the role is built-in and cannot be deleted")
    private Boolean system = Boolean.FALSE;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    @Comment("FALSE when role is disabled")
    private Boolean active = Boolean.TRUE;
}
