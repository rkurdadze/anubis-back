package ge.comcom.anubis.service.core;

import ge.comcom.anubis.config.DocumentPreviewProperties;
import ge.comcom.anubis.service.core.FileService.FileDownload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;

/**
 * Сервис формирования PDF-превью с помощью Gotenberg.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentPreviewService {

    private static final String DEFAULT_FILENAME = "document";

    private final FileService fileService;
    private final RestTemplate gotenbergRestTemplate;
    private final DocumentPreviewProperties properties;

    /**
     * Возвращает PDF-превью для файла.
     */
    public PreviewDocument renderPreview(Long fileId) throws IOException {
        FileDownload download = fileService.loadFile(fileId);
        var file = download.getFile();
        String filename = file.getFileName();
        String mimeType = file.getMimeType();

        if (!properties.isEnabled()) {
            throw new IllegalStateException("Document preview service disabled");
        }

        if (isPdf(mimeType, filename)) {
            return new PreviewDocument(appendPdfExtension(filename), download.getContent());
        }

        byte[] pdfContent = convertToPdf(download);
        return new PreviewDocument(appendPdfExtension(filename), pdfContent);
    }

    private byte[] convertToPdf(FileDownload download) {
        String endpoint = resolveEndpoint(download.getFile().getMimeType(), download.getFile().getFileName());
        String url = buildUrl(endpoint);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        String originalName = download.getFile().getFileName();
        String filename = originalName == null || originalName.isBlank() ? DEFAULT_FILENAME : originalName;
        ContentDisposition contentDisposition = ContentDisposition.formData()
                .name("files")
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        fileHeaders.setContentDisposition(contentDisposition);

        HttpEntity<NamedByteArrayResource> filePart = new HttpEntity<>(
                new NamedByteArrayResource(download.getContent(), filename),
                fileHeaders
        );
        body.add("files", filePart);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_PDF));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<byte[]> response = gotenbergRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    byte[].class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            log.error("Gotenberg conversion failed with status {}", response.getStatusCode());
            throw new IllegalStateException("Failed to convert document to PDF. Status: " + response.getStatusCode());

        } catch (RestClientException ex) {
            log.error("Error calling Gotenberg: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Unable to contact Gotenberg service", ex);
        }
    }

    private String resolveEndpoint(String mimeType, String filename) {
        String safeMime = mimeType != null ? mimeType.toLowerCase(Locale.ROOT) : "";
        String safeName = filename != null ? filename.toLowerCase(Locale.ROOT) : "";

        if (safeMime.contains("html") || safeName.endsWith(".html") || safeName.endsWith(".htm")) {
            return "/forms/chromium/convert/html";
        }

        return "/forms/libreoffice/convert";
    }

    private String buildUrl(String endpoint) {
        String baseUrl = properties.getGotenberg().getBaseUrl();
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + endpoint;
        }
        return baseUrl + endpoint;
    }

    private boolean isPdf(String mimeType, String filename) {
        String safeMime = mimeType != null ? mimeType.toLowerCase(Locale.ROOT) : "";
        if (safeMime.contains("pdf")) {
            return true;
        }
        if (filename == null) {
            return false;
        }
        return filename.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    private String appendPdfExtension(String filename) {
        String baseName = filename == null || filename.isBlank() ? DEFAULT_FILENAME : filename;
        if (baseName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            return baseName;
        }
        int dotIndex = baseName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = baseName.substring(0, dotIndex);
        }
        return baseName + ".pdf";
    }

    public record PreviewDocument(String filename, byte[] content) {
        public ByteArrayResource asResource() {
            return new ByteArrayResource(content);
        }
    }

    private static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}

