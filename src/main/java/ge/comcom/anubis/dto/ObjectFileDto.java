package ge.comcom.anubis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Represents a stored file within the repository, linked to an object version.")
public class ObjectFileDto {

    private Long id;
    private Long objectId;
    private Long versionId;
    private Long binaryId;     // 🔥 новое поле
    private String filename;
    private String mimeType;
    private Long size;
    private boolean deleted;
}
