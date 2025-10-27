package ge.comcom.anubis.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "anubis.ocr")
@Getter
@Setter
public class OcrProperties {
    private boolean enabled = true;
    private String datapath;
    private String languages = "kat+eng+rus";
    private int psm = 3;
    private int oem = 1;
}