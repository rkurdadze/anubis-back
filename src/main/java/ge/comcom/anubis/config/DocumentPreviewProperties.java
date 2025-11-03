package ge.comcom.anubis.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Настройки интеграции с сервисом Gotenberg для конвертации документов в PDF.
 */
@Configuration
@ConfigurationProperties(prefix = "anubis.preview")
@Getter
@Setter
public class DocumentPreviewProperties {

    /**
     * Флаг, позволяющий полностью отключить генерацию превью.
     */
    private boolean enabled = true;

    private final Gotenberg gotenberg = new Gotenberg();

    @Getter
    @Setter
    public static class Gotenberg {
        /**
         * Базовый URL сервиса Gotenberg.
         */
        private String baseUrl = "http://gotenberg:3000";

        /**
         * Таймаут установления соединения.
         */
        private Duration connectTimeout = Duration.ofSeconds(5);

        /**
         * Таймаут ожидания ответа при конвертации.
         */
        private Duration readTimeout = Duration.ofSeconds(60);
    }
}

