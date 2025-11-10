package ge.comcom.anubis.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // ✅ маршруты, на которые сервер может отправлять сообщения
        registry.enableSimpleBroker("/topic", "/queue");
        // ✅ префикс для входящих от клиента сообщений (если будут)
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 🔗 основной endpoint, совпадает с Angular `ws-anubis`
        registry.addEndpoint("/api/ws-anubis")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
