package ge.comcom.anubis.integration.ocr;

import ge.comcom.anubis.config.OcrProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RemoteOcrClient {

    private static final Path DOCKER_ENV = Path.of("/.dockerenv");

    private final WebClient ocrWebClient;
    private final OcrProperties ocrProperties;

    private final boolean insideContainer = Files.exists(DOCKER_ENV);

    @PostConstruct
    void ensureOcrReady() {
        if (!ocrProperties.isEnabled()) {
            log.info("Remote OCR disabled by configuration");
            return;
        }

        log.info("Проверяем готовность OCR-шлюза по адресу {}", ocrProperties.getServiceUrl());
        if (isServiceReady()) {
            log.info("OCR-шлюз доступен. Языки по умолчанию: {}", ocrProperties.getLanguages());
            return;
        }

        log.warn("⚠️ OCR-шлюз недоступен по адресу {}. " +
                        "Проверьте, что контейнер anubis-ocr запущен вручную и порт 4101 открыт.",
                ocrProperties.getServiceUrl());

        waitForService();
        log.info("OCR-шлюз готов к работе: {}", ocrProperties.getServiceUrl());
    }

    public Optional<RemoteOcrResponse> extract(File file, String originalFileName) {
        if (!ocrProperties.isEnabled()) {
            log.debug("Remote OCR disabled by configuration");
            return Optional.empty();
        }
        if (file == null || !file.exists()) {
            log.warn("Cannot send null/non-existing file to OCR service");
            return Optional.empty();
        }

        String fileName = originalFileName != null ? originalFileName : file.getName();
        FileSystemResource resource = new FileSystemResource(file);
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", resource)
                .filename(fileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM);

        Duration timeout = ocrProperties.getRequestTimeout();

        try {
            RemoteOcrResponse response = ocrWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/ocr")
                            .queryParam("languages", ocrProperties.getLanguages())
                            .build())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(RemoteOcrResponse.class)
                    .timeout(timeout)
                    .doOnSubscribe(sub -> log.debug("Отправляем {} в OCR-шлюз {}", fileName, ocrProperties.getServiceUrl()))
                    .doOnError(ex -> log.error("Ошибка OCR-шлюза: {}", ex.getMessage()))
                    .block();

            return Optional.ofNullable(response);
        } catch (Exception ex) {
            log.error("OCR gateway request failed for {}: {}", fileName, ex.getMessage());
            return Optional.empty();
        }
    }

    private boolean isServiceReady() {
        try {
            ocrWebClient.get()
                    .uri("/healthz")
                    .retrieve()
                    .toBodilessEntity()
                    .block(Duration.ofMillis(Math.max(500L, ocrProperties.getConnectTimeout().toMillis())));
            return true;
        } catch (Exception ex) {
            log.debug("Проверка OCR-шлюза не удалась: {}", ex.getMessage());
            return false;
        }
    }

    private void waitForService() {
        Duration timeout = ocrProperties.getReadinessTimeout();
        Duration interval = ocrProperties.getHealthCheckInterval();
        Instant deadline = Instant.now().plus(timeout);

        while (Instant.now().isBefore(deadline)) {
            if (isServiceReady()) {
                return;
            }
            try {
                Thread.sleep(Math.max(200L, interval.toMillis()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Ожидание готовности OCR-шлюза прервано", e);
            }
        }
        throw new IllegalStateException("OCR-шлюз не стал готовым за " + timeout.toSeconds() + " с");
    }
}
