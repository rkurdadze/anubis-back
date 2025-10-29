package ge.comcom.anubis.entity.meta;


import ge.comcom.anubis.entity.ActivatableEntity;
import ge.comcom.anubis.entity.core.ObjectClass;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

/**
 * Defines which properties are available in a class and UI-related rules.
 * Links ObjectClass <-> PropertyDef.
 */
@Entity
@Table(
        name = "class_property",
        uniqueConstraints = @UniqueConstraint(
                name = "class_property_class_property_def_key",
                columnNames = {"class_id", "property_def_id"}
        )
)
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Class-property binding (property availability and UI hints)")
public class ClassProperty implements ActivatableEntity {

    @EmbeddedId
    private ClassPropertyId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("classId")
    @JoinColumn(name = "class_id", nullable = false,
            foreignKey = @ForeignKey(name = "class_property_class_id_fkey"))
    @Schema(description = "Linked class")
    private ObjectClass objectClass;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("propertyDefId")
    @JoinColumn(name = "property_def_id", nullable = false,
            foreignKey = @ForeignKey(name = "class_property_property_def_id_fkey"))
    @Schema(description = "Linked property definition")
    private PropertyDef propertyDef;

    @Builder.Default
    @Column(name = "is_readonly", nullable = false)
    @Schema(description = "If TRUE, property is displayed as read-only in UI", example = "false")
    private Boolean isReadonly = false;

    @Builder.Default
    @Column(name = "is_hidden", nullable = false)
    @Schema(description = "If TRUE, property is hidden in UI", example = "false")
    private Boolean isHidden = false;

    @Column(name = "display_order")
    @Schema(description = "UI order for display", example = "10")
    private Integer displayOrder;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public Long getClassId() {
        return id != null ? id.getClassId() : null;
    }

    public Long getPropertyDefId() {
        return id != null ? id.getPropertyDefId() : null;
    }

    @Override
    public Boolean getIsActive() { return isActive; }
    @Override
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

}
