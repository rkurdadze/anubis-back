package ge.comcom.anubis.entity.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ge.comcom.anubis.enums.LinkDirection;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

/**
 * Defines reusable relationship roles between objects.
 * Equivalent to M-Files "Relationship Definition".
 */
@Entity
@Table(name = "object_link_role")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class LinkRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary key. Example: 10.")
    @Column(name = "role_id")
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    @Comment("Role name. Example: 'Customer'.")
    private String name;

    @Column(name = "name_i18n")
    @Comment("Localized name(s), e.g. {\"en\":\"Customer\",\"ru\":\"Клиент\"}.")
    private String nameI18n;

    @Column(name = "description")
    @Comment("Optional textual description for UI and documentation.")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    @Comment("Default link direction for this role (UNI or BI).")
    private LinkDirection direction = LinkDirection.UNI;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "src_object_type_id")
    @Comment("Optional restriction: allowed source object type.")
    @JsonIgnoreProperties({"attributes", "objectClass"})
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ObjectType sourceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dst_object_type_id")
    @Comment("Optional restriction: allowed destination object type.")
    @JsonIgnoreProperties({"attributes", "objectClass"})
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ObjectType targetType;

    @Column(name = "is_active", nullable = false)
    @Comment("TRUE if the role is available for use.")
    private Boolean isActive = true;
}
