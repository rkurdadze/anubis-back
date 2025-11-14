package ge.comcom.anubis.entity.core;

import ge.comcom.anubis.entity.security.Acl;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a class definition inside the metadata structure.
 * Each class belongs to one ObjectType and defines metadata schema rules.
 */
@Entity
@Table(name = "\"class\"",
        uniqueConstraints = @UniqueConstraint(name = "class_object_type_id_name_key",
                columnNames = {"object_type_id", "name"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class ObjectClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "class_id")
    @Schema(description = "Unique identifier of the class", example = "42")
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @Column(nullable = false)
    @Schema(description = "Human-readable class name", example = "Broadcast Station")
    @ToString.Include
    private String name;

    @Column
    @Schema(description = "Optional class description", example = "Represents a radio broadcasting station entity")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "object_type_id", nullable = false,
            foreignKey = @ForeignKey(name = "class_object_type_id_fkey"))
    @Schema(description = "Linked ObjectType")
    private ObjectType objectType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acl_id",
            foreignKey = @ForeignKey(name = "class_acl_id_fkey"))
    @Schema(description = "Access control list associated with this class")
    private Acl acl;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    @Schema(description = "Defines whether the class is active", example = "true")
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_class_id",
            foreignKey = @ForeignKey(name = "class_parent_class_id_fkey"))
    @Schema(description = "Parent class in hierarchy")
    private ObjectClass parent;

    @OneToMany(mappedBy = "parent")
    @Builder.Default
    private List<ObjectClass> children = new ArrayList<>();
}
