package ge.comcom.anubis.dto.meta;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO for ValueListItem")
public class ValueListItemDto {

    private Long id;
    private Long valueListId;
    private String valueText;
    private String valueTextI18n;
    private Integer sortOrder;
    private Boolean isActive;
    private Long parentItemId;
    private String externalCode;
}
