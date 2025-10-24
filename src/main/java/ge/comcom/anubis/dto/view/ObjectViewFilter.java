package ge.comcom.anubis.dto.view;

import lombok.*;

/**
 * Represents a single filter condition stored in filter_json.
 * Example: { "propertyDefId": 50, "op": "=", "value": "Active" }
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObjectViewFilter {

    private Long propertyDefId;
    private String op;   // =, !=, >, <, LIKE, IN, BETWEEN, ISNULL
    private String value;
    private String valueTo;
}
