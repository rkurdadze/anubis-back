package ge.comcom.anubis.dto.core;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Represents a stored file within the repository, linked to an object version.")
public class ObjectFileDto {

    @Schema(description = "Unique file ID", example = "12")
    private Long id;

    @Schema(description = "Associated object ID", example = "5")
    private Long objectId;

    @Schema(description = "Associated object version ID", example = "8")
    private Long versionId;

    @Schema(description = "Original file name", example = "contract_2025_signed.pdf")
    private String filename;

    @Schema(description = "MIME type of the file", example = "application/pdf")
    private String mimeType;

    @Schema(description = "File size in bytes", example = "254698")
    private Long size;

    @Schema(description = "Upload timestamp (ISO 8601)", example = "2025-10-24T13:45:00Z")
    private String uploadedAt;
}
