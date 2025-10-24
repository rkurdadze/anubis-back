package ge.comcom.anubis.entity.core;

import jakarta.persistence.*;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

/**
 * Single item within a ValueList.
 * Supports hierarchy (parent_item_id), ordering, and activity flag.
 */
@Entity
@Table(
        name = "value_list_item",
        uniqueConstraints = @UniqueConstraint(
                name = "value_list_id_value_text_key",
                columnNames = {"value_list_id", "value_text"}
        )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Value List Item (element within dictionary)")
public class ValueListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    @Schema(description = "Primary key", example = "101")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "value_list_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "value_list_item_value_list_id_fkey")
    )
    @Schema(description = "Linked value list")
    private ValueList valueList;

    @Column(name = "value_text", nullable = false)
    @Schema(description = "Display text", example = "Approved")
    private String valueText;

    @Column(name = "value_text_i18n", columnDefinition = "jsonb")
    @Schema(description = "Localized text (JSON)", example = "{\"en\":\"Approved\",\"ru\":\"Согласовано\"}")
    private String valueTextI18n;

    @Column(name = "sort_order")
    @Schema(description = "Order number", example = "10")
    private Integer sortOrder;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    @Schema(description = "TRUE=visible, FALSE=hidden", example = "true")
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "parent_item_id",
            foreignKey = @ForeignKey(name = "value_list_item_parent_item_id_fkey")
    )
    @Schema(description = "Parent item (for hierarchical lists)")
    private ValueListItem parentItem;

    @Column(name = "external_code")
    @Schema(description = "Integration/external code", example = "APPROVED_200")
    private String externalCode;
}

