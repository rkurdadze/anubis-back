package ge.comcom.anubis.dto;

import ge.comcom.anubis.enums.LinkDirection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Relationship between two repository objects")
public class ObjectLinkDto {

    @Schema(description = "Link identifier", example = "42")
    private Long id;

    @Schema(description = "Source object identifier", example = "1001")
    private Long sourceId;

    @Schema(description = "Target object identifier", example = "1002")
    private Long targetId;

    @Schema(description = "Role identifier", example = "7")
    private Long roleId;

    @Schema(description = "Role name", example = "Customer")
    private String roleName;

    @Schema(description = "Link direction", example = "UNI")
    private LinkDirection direction;
}
