package ge.comcom.anubis.service;

import ge.comcom.anubis.dto.ws.WsEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocketNotifierService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Отправляет сообщение в указанный топик.
     * @param topic — путь, например "/topic/files/123"
     * @param type — тип события, например "FILE_STATUS"
     * @param payload — данные (DTO)
     */
    public <T> void send(String topic, String type, T payload) {
        WsEnvelope<T> envelope = new WsEnvelope<>(
                type,
                payload,
                topic,
                System.currentTimeMillis()
        );
        try {
            messagingTemplate.convertAndSend(topic, envelope);
            log.info("[WS] Sent to {} type={} payload={}", topic, type, payload);
        } catch (Exception e) {
            log.error("[WS] Failed to send to {}: {}", topic, e.getMessage());
        }
    }

    /**
     * Упрощённый алиас для частых случаев
     */
    public <T> void toFileVersion(Long versionId, String type, T payload) {
        send("/topic/files/" + versionId, type, payload);
    }

    /**
     * 🔹 Отправить сообщение всем подписчикам — глобально
     * Используй этот метод, если Angular подписан на /topic/files/all
     */
    public <T> void toAllFiles(String type, T payload) {
        send("/topic/files/all", type, payload);
    }
}

