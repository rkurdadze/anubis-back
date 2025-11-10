package ge.comcom.anubis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WsEnvelope<T> {
    private String type;      // например: FILE_STATUS, PROGRESS, WORKFLOW
    private T payload;        // любое тело (DTO, Map, Error и т.п.)
    private String topic;     // опционально, для логирования
    private long timestamp;   // время отправки
}

