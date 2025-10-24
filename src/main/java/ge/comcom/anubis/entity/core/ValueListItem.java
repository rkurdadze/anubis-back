package ge.comcom.anubis.entity.core;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

/**
 * Represents an item within a specific ValueList.
 * Used by properties with VALUELIST or MULTI_VALUELIST types.
 */
@Entity
@Table(
        name = "value_list_item",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "value_list_item_value_list_id_value_text_key",
                        columnNames = {"value_list_id", "value_text"}
                )
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Single selectable item within a ValueList")
public class ValueListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    @Schema(description = "Primary key", example = "105")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "value_list_id", nullable = false,
            foreignKey = @ForeignKey(name = "value_list_item_value_list_id_fkey"))
    @Schema(description = "Parent ValueList reference")
    private ValueList valueList;

    @Column(name = "value_text", nullable = false)
    @Schema(description = "Display text of the item", example = "Approved")
    private String value;

    @Column(name = "value_text_i18n", columnDefinition = "jsonb")
    @Schema(description = "Localized value (JSON)", example = "{\"en\":\"Approved\",\"ru\":\"Одобрено\"}")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> valueI18n;

    @Column(name = "sort_order")
    @Schema(description = "Sorting order", example = "10")
    private Integer sortOrder;

    @Column(name = "is_active")
    @Schema(description = "Active state flag", example = "true")
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_item_id",
            foreignKey = @ForeignKey(name = "value_list_item_parent_item_id_fkey"))
    @Schema(description = "Optional parent item (for hierarchical lists)")
    private ValueListItem parentItem;

    @Column(name = "external_code")
    @Schema(description = "Integration or external system code", example = "APPROVED_200")
    private String externalCode;
}
