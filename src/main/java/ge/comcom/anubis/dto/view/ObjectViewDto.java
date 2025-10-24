package ge.comcom.anubis.dto.view;

import lombok.*;

import java.util.List;

/**
 * DTO for returning and creating ObjectView data.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObjectViewDto {

    private Long id;
    private String name;
    private Boolean isCommon;
    private Long createdById;
    private String filterJson;
    private Integer sortOrder;
    private List<ViewGroupingDto> groupings;
}
