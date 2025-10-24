package ge.comcom.anubis.dto.core;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObjectDto {
    @Schema(description = "Unique identifier of the object", example = "1")
    private Long id;

    @Schema(description = "Display name of the object (document, project, etc.)", example = "Contract_2025_001")
    private String name;

    @Schema(description = "Reference to object type ID", example = "2")
    private Long objectTypeId;

    @Schema(description = "Reference to class ID", example = "5")
    private Long objectClassId;

    @Schema(description = "Name of the object type", example = "Document")
    private String objectTypeName;

    @Schema(description = "Name of the class within the type", example = "Contract")
    private String objectClassName;
}

