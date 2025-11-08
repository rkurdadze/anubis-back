package ge.comcom.anubis.service.core;

import ge.comcom.anubis.config.DocumentPreviewProperties;
import ge.comcom.anubis.service.core.FileService.FileDownload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.imaging.Imaging;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
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

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Iterator;
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

        if ((mimeType != null && mimeType.contains("image")) ||
                filename.endsWith(".psd") || filename.endsWith(".tif") ||
                filename.endsWith(".tiff") || filename.endsWith(".png") ||
                filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {

            try {
                log.debug("🖼️ Local image→PDF conversion for {}", filename);
                byte[] pdfBytes = convertPsdToPdf(download.getContent(), filename);
                return new PreviewDocument(appendPdfExtension(filename), pdfBytes);
            } catch (IOException e) {
                log.warn("⚠️ Local PSD conversion failed ({}), fallback to Gotenberg", e.getMessage());
                byte[] pdfContent = convertToPdf(download);
                return new PreviewDocument(appendPdfExtension(filename), pdfContent);
            }

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

    /**
     * Проверяет, является ли изображение практически полностью чёрным.
     * Используется для выявления PSD без merged preview.
     */
    private boolean isBlackImage(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();

        // Чтобы не было слишком медленно — берём не все пиксели, а сетку 20x20
        int stepX = Math.max(1, w / 20);
        int stepY = Math.max(1, h / 20);

        long total = 0;
        long black = 0;

        for (int y = 0; y < h; y += stepY) {
            for (int x = 0; x < w; x += stepX) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                total++;
                // считаем пиксель "чёрным", если он очень тёмный
                if (r < 10 && g < 10 && b < 10) {
                    black++;
                }
            }
        }

        // если >95% пикселей тёмные — считаем изображение чёрным
        return total > 0 && (black * 100 / total) > 95;
    }

    /**
     * Второй способ чтения PSD через ImageIO (если Apache Imaging не справился).
     */
    private BufferedImage readPsdWithImageIO(byte[] bytes) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            reader.setInput(input);
            BufferedImage img = reader.read(0);
            reader.dispose();
            return img;
        }
    }



    /**
     * Локальная конвертация PSD → PDF с использованием ImageIO и PDFBox.
     */
    private byte[] convertPsdToPdf(byte[] psdBytes, String filename) throws IOException {
        BufferedImage image;

        try {
            image = Imaging.getBufferedImage(psdBytes);
            log.debug("✅ Loaded PSD via Apache Commons Imaging: {}", filename);
        } catch (Exception e) {
            throw new IOException("Failed to read PSD using Commons Imaging: " + e.getMessage(), e);
        }


        if (image == null || isBlackImage(image)) {
            log.warn("⚠️ PSD preview missing or black: {} → fallback to ImageIO", filename);
            try {
                image = readPsdWithImageIO(psdBytes);
                if (image == null) {
                    throw new IOException("ImageIO could not read PSD");
                }
            } catch (Exception ex) {
                log.warn("❌ ImageIO fallback failed: {}", ex.getMessage());
                throw new IOException("PSD has no merged preview layer");
            }
        }



        // flatten transparency with proper alpha handling
        BufferedImage flattened = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = flattened.createGraphics();
        g.setComposite(AlphaComposite.SrcOver);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.drawImage(image, 0, 0, null);
        g.dispose();
        image = flattened;

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(image.getWidth(), image.getHeight()));
            doc.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(doc, page)) {
                var pdImage = LosslessFactory.createFromImage(doc, image);
                content.drawImage(pdImage, 0, 0, image.getWidth(), image.getHeight());
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }



}

