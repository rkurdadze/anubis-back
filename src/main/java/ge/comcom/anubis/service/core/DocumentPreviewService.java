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
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.http.ContentDisposition;
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
 * При ошибке Gotenberg — возвращает no_preview.jpg из assets.
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
     * Возвращает превью: PDF или fallback-изображение (no_preview.jpg)
     */
    public PreviewDocument renderPreview(Long fileId) throws IOException {
        FileDownload download = fileService.loadFile(fileId);
        var file = download.getFile();
        String filename = file.getFileName();
        String mimeType = file.getMimeType();

        if (!properties.isEnabled()) {
            throw new IllegalStateException("Document preview service disabled");
        }

        // 1. PDF — пропускаем как есть
        if (isPdf(mimeType, filename)) {
            return new PreviewDocument(appendPdfExtension(filename), download.getContent());
        }

        // 2. Изображения (включая PSD, TIFF) — локальная конвертация
        if (isImageFile(mimeType, filename)) {
            try {
                log.debug("Local image to PDF conversion for {}", filename);
                byte[] pdfBytes = convertPsdToPdf(download.getContent(), filename);
                return new PreviewDocument(appendPdfExtension(filename), pdfBytes);
            } catch (IOException e) {
                log.warn("Local image conversion failed ({}), fallback to Gotenberg", e.getMessage());
                return convertToPdfWithFallback(download);
            }
        }

        // 3. Остальные — через Gotenberg с fallback
        return convertToPdfWithFallback(download);
    }

    /**
     * Пытается конвертировать через Gotenberg, при ошибке — возвращает no_preview.jpg
     */
    private PreviewDocument convertToPdfWithFallback(FileDownload download) {
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
                String pdfFilename = appendPdfExtension(filename);
                return new PreviewDocument(pdfFilename, response.getBody());
            }

            log.warn("Gotenberg conversion failed with status {}, using no_preview.jpg", response.getStatusCode());

        } catch (RestClientException ex) {
            log.warn("Gotenberg unreachable: {}, using no_preview.jpg", ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error during Gotenberg call: {}", ex.getMessage(), ex);
        }

        // === FALLBACK: no_preview.jpg ===
        return loadFallbackPreview();
    }

    /**
     * Загружает no_preview.jpg из classpath: src/main/resources/assets/no_preview.jpg
     */
    private PreviewDocument loadFallbackPreview() {
        try {
            Resource resource = new ClassPathResource("assets/no_preview.jpg");
            if (!resource.exists()) {
                log.error("Fallback image 'assets/no_preview.jpg' not found in classpath");
                throw new IllegalStateException("Fallback preview image missing");
            }
            byte[] bytes = resource.getInputStream().readAllBytes();
            log.info("Using fallback preview: no_preview.jpg");
            return new PreviewDocument("no_preview.jpg", bytes);
        } catch (IOException e) {
            log.error("Failed to load fallback image 'no_preview.jpg'", e);
            throw new IllegalStateException("Preview unavailable and fallback image missing", e);
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

    private boolean isImageFile(String mimeType, String filename) {
        if (mimeType != null && mimeType.toLowerCase(Locale.ROOT).contains("image")) {
            return true;
        }
        if (filename == null) return false;
        String lower = filename.toLowerCase(Locale.ROOT);
        return lower.endsWith(".psd") || lower.endsWith(".tif") || lower.endsWith(".tiff") ||
                lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                lower.endsWith(".gif") || lower.endsWith(".bmp") || lower.endsWith(".webp");
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

    /**
     * Enhanced PreviewDocument with automatic MIME type detection
     */
    public record PreviewDocument(String filename, byte[] content, MediaType mediaType) {
        public PreviewDocument(String filename, byte[] content) {
            this(filename, content,
                    filename.toLowerCase().endsWith(".pdf")
                            ? MediaType.APPLICATION_PDF
                            : MediaType.IMAGE_JPEG);
        }

        public ByteArrayResource asResource() {
            return new ByteArrayResource(content) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };
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

    private boolean isBlackImage(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
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
                if (r < 10 && g < 10 && b < 10) {
                    black++;
                }
            }
        }

        return total > 0 && (black * 100 / total) > 95;
    }

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

    private byte[] convertPsdToPdf(byte[] psdBytes, String filename) throws IOException {
        BufferedImage image = readPsdWithImageIO(psdBytes);
        if (image == null) {
            throw new IOException("Failed to read PSD: no merged preview layer");
        }

        // Ensure ARGB
        BufferedImage argbImage = toArgb(image);

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(argbImage.getWidth(), argbImage.getHeight()));
            doc.addPage(page);

            // Encode as PNG to preserve alpha
            ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
            if (!ImageIO.write(argbImage, "png", pngOut)) {
                throw new IOException("Failed to encode image as PNG");
            }

            PDImageXObject pdImage = PDImageXObject.createFromByteArray(
                    doc, pngOut.toByteArray(), filename
            );

            try (PDPageContentStream content = new PDPageContentStream(doc, page)) {
                content.drawImage(pdImage, 0, 0, argbImage.getWidth(), argbImage.getHeight());
            }

            ByteArrayOutputStream pdfOut = new ByteArrayOutputStream();
            doc.save(pdfOut);
            return pdfOut.toByteArray();
        }
    }


    private BufferedImage toArgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_ARGB) {
            return src;
        }
        BufferedImage argb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = argb.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return argb;
    }
}