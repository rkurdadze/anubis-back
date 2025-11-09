package ge.comcom.anubis.integration.ocr;

import ge.comcom.anubis.config.OcrProperties;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
public class OcrClientConfiguration {

    @Bean
    public WebClient ocrWebClient(WebClient.Builder builder, OcrProperties ocrProperties) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) ocrProperties.getConnectTimeout().toMillis())
                .responseTimeout(ocrProperties.getRequestTimeout())
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(ocrProperties.getReadTimeout().toMillis(), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(ocrProperties.getConnectTimeout().toMillis(), TimeUnit.MILLISECONDS)));

        return builder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(ocrProperties.getServiceUrl())
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(100 * 1024 * 1024))
                        .build())
                .build();
    }
}
