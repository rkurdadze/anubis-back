package ge.comcom.anubis.entity.security;

import jakarta.persistence.*;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents Access Control List (ACL) entity.
 * Each ACL defines a permission set that can be linked to objects or classes.
 */
@Entity
@Table(name = "acl")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Access Control List entity")
public class Acl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "acl_id")
    @Schema(description = "Unique identifier of the ACL", example = "15")
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 255)
    @Schema(description = "Human-readable name of the ACL", example = "Default Class Permissions")
    private String name;
}
