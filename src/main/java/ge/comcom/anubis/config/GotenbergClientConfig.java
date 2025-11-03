package ge.comcom.anubis.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Конфигурация HTTP-клиента для обращения к Gotenberg.
 */
@Configuration
@RequiredArgsConstructor
public class GotenbergClientConfig {

    private final DocumentPreviewProperties properties;

    @Bean
    public RestTemplate gotenbergRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int connectTimeout = (int) properties.getGotenberg().getConnectTimeout().toMillis();
        int readTimeout = (int) properties.getGotenberg().getReadTimeout().toMillis();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return new RestTemplate(factory);
    }
}

