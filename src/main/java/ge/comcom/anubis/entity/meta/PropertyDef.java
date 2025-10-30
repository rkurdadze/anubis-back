package ge.comcom.anubis.entity.meta;

import com.vladmihalcea.hibernate.type.json.JsonType;
import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import ge.comcom.anubis.entity.ActivatableEntity;
import ge.comcom.anubis.entity.core.ObjectType;
import ge.comcom.anubis.entity.core.ValueList;
import ge.comcom.anubis.enums.PropertyDataType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;


import java.util.Map;

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
public class PropertyDef implements ActivatableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "property_def_id")
    @Schema(description = "Primary key", example = "50")
    private Long id;

    @Column(nullable = false)
    @Schema(description = "Technical property name (key)", example = "CustomerName")
    private String name;


    @Type(JsonType.class) // если используешь Hibernate Types
    @Column(name = "caption_i18n", columnDefinition = "jsonb")
    @Schema(description = "Localized captions as JSON", example = "{\"en\":\"Customer Name\",\"ru\":\"Имя клиента\"}")
    private Map<String, String> captionI18n;


//    @Enumerated(EnumType.STRING)
//    @Column(name = "data_type", nullable = false, columnDefinition = "data_type_enum")
//    @Schema(description = "Data type: TEXT, NUMBER, DATE, BOOLEAN, LOOKUP, VALUELIST", example = "TEXT")
//    private PropertyDataType dataType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "data_type", nullable = false, columnDefinition = "data_type_enum")
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

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Override
    public Boolean getIsActive() { return isActive; }
    @Override
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

}
