package ge.comcom.anubis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Object with its incoming and outgoing links")
public class ObjectLinksDto {

    @Schema(description = "Object summary")
    private ObjectDto object;

    @Schema(description = "Outgoing relationships")
    private List<ObjectLinkDto> outgoing;

    @Schema(description = "Incoming relationships")
    private List<ObjectLinkDto> incoming;
}
