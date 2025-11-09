package ge.comcom.anubis.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "anubis.ocr")
@Getter
@Setter
public class OcrProperties {
    private boolean enabled = true;
    private String serviceUrl = "http://localhost:4101";
    private String languages = "kat+eng+rus";

    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(60);
    private Duration requestTimeout = Duration.ofSeconds(90);

    private boolean autoStartContainer = true;
    private String dockerComposeFile = "docker/ocr/docker-compose.yml";
    private String dockerComposeService = "ocr-gateway";
    private Duration readinessTimeout = Duration.ofSeconds(120);
    private Duration healthCheckInterval = Duration.ofSeconds(2);
}
