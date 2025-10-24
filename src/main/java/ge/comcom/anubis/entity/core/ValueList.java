package ge.comcom.anubis.entity.core;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dictionary / picklist used by VALUELIST-type properties.
 */
@Entity
@Table(name = "value_list")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Value List (dictionary of selectable items)")
public class ValueList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "value_list_id")
    @Schema(description = "Primary key", example = "10")
    private Long id;

    @Column(nullable = false, unique = true)
    @Schema(description = "Internal technical name of the list", example = "DocumentStatus")
    private String name;

    @Column(name = "name_i18n", columnDefinition = "jsonb")
    @Schema(description = "Localized name (JSON)", example = "{\"en\":\"Status\",\"ru\":\"Статус\"}")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> nameI18n;

    @OneToMany(mappedBy = "valueList", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    @Schema(description = "List of selectable items")
    @Builder.Default
    private List<ValueListItem> items = new ArrayList<>();
}
