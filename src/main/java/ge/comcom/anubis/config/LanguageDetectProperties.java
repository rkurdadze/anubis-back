package ge.comcom.anubis.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "anubis.language-detect")
@Getter
@Setter
public class LanguageDetectProperties {
    private boolean enabled = true;
}