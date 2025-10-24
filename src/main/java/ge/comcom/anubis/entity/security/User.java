package ge.comcom.anubis.entity.security;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "\"user\"")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String username; // e.g. "rkurdadze"

    @Column(name = "full_name")
    private String fullName; // e.g. "Roman Kurdadze"

    @Column(name = "password_hash")
    private String passwordHash; // e.g. bcrypt hash or token
}
