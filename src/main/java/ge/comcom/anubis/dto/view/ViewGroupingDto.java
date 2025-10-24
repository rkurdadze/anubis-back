package ge.comcom.anubis.dto.view;

import lombok.*;

/**
 * Grouping level DTO (property + level).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewGroupingDto {

    private Integer level;
    private Long propertyDefId;
}
