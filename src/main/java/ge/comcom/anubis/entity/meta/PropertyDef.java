package ge.comcom.anubis.entity.meta;

import ge.comcom.anubis.entity.core.ObjectType;
import ge.comcom.anubis.entity.core.ValueList;
import ge.comcom.anubis.enums.PropertyDataType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

/**
 * Property definition (EAV schema).
 * Defines metadata field available to objects of specific classes.
 */
@Entity
@Table(name = "property_def")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Property Definition (metadata field definition)")
public class PropertyDef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "property_def_id")
    @Schema(description = "Primary key", example = "50")
    private Long id;

    @Column(nullable = false)
    @Schema(description = "Technical property name (key)", example = "CustomerName")
    private String name;

    @Column(name = "caption_i18n", columnDefinition = "jsonb")
    @Schema(description = "Localized captions as JSON", example = "{\"en\":\"Customer Name\",\"ru\":\"Имя клиента\"}")
    private String captionI18n;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false)
    @Schema(description = "Data type: TEXT, NUMBER, DATE, BOOLEAN, LOOKUP, VALUELIST", example = "TEXT")
    private PropertyDataType dataType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ref_object_type_id",
            foreignKey = @ForeignKey(name = "property_def_ref_object_type_id_fkey"))
    @Schema(description = "Referenced ObjectType (for LOOKUP data type)")
    private ObjectType refObjectType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "value_list_id",
            foreignKey = @ForeignKey(name = "property_def_value_list_id_fkey"))
    @Schema(description = "Linked ValueList (for VALUELIST data type)")
    private ValueList valueList;

    @Builder.Default
    @Column(name = "is_multiselect", nullable = false)
    @Schema(description = "Allows multiple values", example = "false")
    private Boolean isMultiselect = false;

    @Builder.Default
    @Column(name = "is_required", nullable = false)
    @Schema(description = "Property is mandatory", example = "false")
    private Boolean isRequired = false;

    @Builder.Default
    @Column(name = "is_unique", nullable = false)
    @Schema(description = "Must be unique across class scope", example = "false")
    private Boolean isUnique = false;

    @Column
    @Schema(description = "Regex validation pattern", example = "^[A-Z0-9_-]{3,20}$")
    private String regex;

    @Column(name = "default_value")
    @Schema(description = "Default value (as string)", example = "Draft")
    private String defaultValue;

    @Column
    @Schema(description = "Description of the property purpose", example = "Approval status field")
    private String description;
}
