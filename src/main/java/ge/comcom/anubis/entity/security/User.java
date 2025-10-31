package ge.comcom.anubis.entity.security;

import ge.comcom.anubis.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

/**
 * Represents a system user.
 * Matches table "user" in the database.
 */
@Entity
@Table(
        name = "\"user\"",
        uniqueConstraints = {
                @UniqueConstraint(name = "user_username_key", columnNames = {"username"})
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    @Comment("Primary key of the user")
    private Long id;

    @Column(name = "username", nullable = false, unique = true)
    @Comment("Unique login username")
    private String username;

    @Column(name = "full_name")
    @Comment("Full display name of the user")
    private String fullName;

    @Column(name = "password_hash")
    @Comment("Optional password hash (for non-SSO authentication)")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Comment("Account status (ACTIVE/INACTIVE/LOCKED)")
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;
}
