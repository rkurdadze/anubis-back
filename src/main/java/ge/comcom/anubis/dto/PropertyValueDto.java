package ge.comcom.anubis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Универсальный DTO для свойств — поддерживает single и multi значения.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyValueDto {
    private Long propertyDefId;
    private Object value;  // String, Number, Boolean или List<Object>
}
