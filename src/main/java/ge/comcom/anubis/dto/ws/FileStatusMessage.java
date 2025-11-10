package ge.comcom.anubis.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileStatusMessage {
    private Long fileId;
    private Long objectVersionId;
    private String status;    // INDEXED, PENDING, FAILED
    private String message;
}
