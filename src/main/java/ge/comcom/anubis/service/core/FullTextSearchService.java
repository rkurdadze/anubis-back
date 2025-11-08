package ge.comcom.anubis.service.core;

import ge.comcom.anubis.config.LanguageDetectProperties;
import ge.comcom.anubis.config.OcrProperties;
import ge.comcom.anubis.entity.core.ObjectFileEntity;
import ge.comcom.anubis.entity.core.SearchTextCache;
import ge.comcom.anubis.repository.core.ObjectFileRepository;
import ge.comcom.anubis.repository.core.SearchTextCacheRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;

import javax.imageio.ImageIO;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Реализация полнотекстового поиска для Anubis.
 * Поддерживает:
 * - Apache Tika для текстовых форматов (PDF, DOCX, XLSX, HTML и т.д.)
 * - Tesseract OCR для изображений и сканов
 * - Inline (BYTEA) и внешние файлы (FS/S3)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FullTextSearchService {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "bmp", "tif", "tiff", "heic", "webp");
    private static final Set<String> PDF_EXTENSIONS = Set.of("pdf");

    private final ObjectFileRepository fileRepository;
    private final SearchTextCacheRepository cacheRepository;
    private final OcrProperties ocrProperties;
    private final LanguageDetectProperties languageDetectProperties;

    private final Tika tika = new Tika();
    private LanguageDetector languageDetector;

    @PersistenceContext
    private EntityManager em;

    // =============================================================
    // 🧠 ThreadLocal-кэширование экземпляров Tesseract (безопасно!)
    // =============================================================
    private final ThreadLocal<Tesseract> ocrThreadLocal = ThreadLocal.withInitial(() -> {
        Tesseract t = new Tesseract();
        String tessdataPath = System.getenv().getOrDefault("TESSDATA_PREFIX",
                "/usr/share/tesseract-ocr/tessdata");
        t.setDatapath(tessdataPath);
        t.setLanguage("kat+eng+rus"); // будет переопределено при initEngines()
        t.setPageSegMode(3);
        t.setOcrEngineMode(1);
        try {
            t.setTessVariable("preserve_interword_spaces", "1");
            t.setTessVariable("user_defined_dpi", "300");
        } catch (Exception e) {
            log.warn("⚠️ Failed to set default Tesseract vars: {}", e.getMessage());
        }
        return t;
    });

    private Tesseract getTesseract() {
        Tesseract t = ocrThreadLocal.get();
        String datapath = System.getProperty("TESSDATA_PREFIX");

        if (datapath == null || datapath.isBlank()) {
            datapath = "/opt/homebrew/share/tessdata";
            System.setProperty("TESSDATA_PREFIX", datapath);
            log.warn("⚠️ TESSDATA_PREFIX not set, fallback to {}", datapath);
        }

        File engFile = new File(datapath, "eng.traineddata");
        if (!engFile.exists()) {
            log.error("❌ Missing traineddata files in {}", datapath);
            throw new IllegalStateException("Tesseract traineddata not found in " + datapath);
        }

        t.setDatapath(datapath);
        t.setLanguage(ocrProperties.getLanguages());
        t.setPageSegMode(ocrProperties.getPsm());
        t.setOcrEngineMode(ocrProperties.getOem());
        return t;
    }


    @PostConstruct
    private void initEngines() {
        // PDFBox настройка
        try {
            String fontDirs = System.getenv().getOrDefault("PDFBOX_FONTDIR", "/Library/Fonts:/System/Library/Fonts");
            String fontCache = "/tmp/pdf-font-cache";
            System.setProperty("pdfbox.fontdir", fontDirs);
            System.setProperty("pdfbox.fontcache", fontCache);
            System.setProperty("org.apache.pdfbox.rendering.UseFallbackFonts", "true");
            log.info("🖋 PDFBox fonts configured: fontdir={}, fontcache={}, fallbackFonts=true", fontDirs, fontCache);
        } catch (Exception e) {
            log.warn("⚠️ PDFBox font initialization failed: {}", e.getMessage());
        }

        // Language detector
        if (languageDetectProperties.isEnabled()) {
            try {
                languageDetector = new OptimaizeLangDetector().loadModels();
                log.info("Language detector initialized (Optimaize)");
            } catch (Exception e) {
                languageDetector = null;
                log.warn("Language detector failed to initialize: {}", e.getMessage());
            }
        }

        // OCR system env
        if (!ocrProperties.isEnabled()) {
            log.info("OCR disabled in configuration.");
            return;
        }

        try {
            String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
            boolean isContainer = Files.exists(Path.of("/.dockerenv"));
            String tessdataPath = null;
            String libPath = null;

            if (os.contains("mac") || os.contains("darwin")) {
                // --- macOS support (Intel и ARM) ---
                Path[] dataCandidates = {
                        Path.of("/opt/homebrew/share/tessdata"),   // M1/M2
                        Path.of("/usr/local/share/tessdata"),       // Intel Mac
                        Path.of("/usr/share/tessdata")              // fallback
                };
                tessdataPath = Stream.of(dataCandidates)
                        .filter(Files::isDirectory)
                        .map(Path::toString)
                        .findFirst()
                        .orElse("/usr/local/share/tessdata");

                Path[] libCandidates = {
                        Path.of("/opt/homebrew/lib"),
                        Path.of("/usr/local/lib"),
                        Path.of("/usr/lib")
                };
                libPath = Stream.of(libCandidates)
                        .filter(Files::isDirectory)
                        .map(Path::toString)
                        .findFirst().orElse(null);

                if (libPath != null) {
                    System.setProperty("jna.library.path", libPath);
                    System.setProperty("DYLD_LIBRARY_PATH", libPath);
                }
                log.info("✅ Detected macOS — using tessdata={} and library={}", tessdataPath, libPath);

            } else if (os.contains("linux")) {
                // --- Linux (включая Docker) ---
                Path[] dataCandidates = {
                        Path.of("/usr/share/tesseract-ocr/5/tessdata"),
                        Path.of("/usr/share/tesseract-ocr/tessdata"),
                        Path.of("/usr/local/share/tessdata")
                };
                tessdataPath = Stream.of(dataCandidates)
                        .filter(Files::isDirectory)
                        .map(Path::toString)
                        .findFirst()
                        .orElse("/usr/share/tesseract-ocr/tessdata");

                libPath = "/usr/lib/x86_64-linux-gnu";
                System.setProperty("jna.library.path", libPath);
                System.setProperty("LD_LIBRARY_PATH", libPath);
                log.info("✅ Detected Linux — tessdata={}, lib={}", tessdataPath, libPath);

            } else if (os.contains("win")) {
                // --- Windows ---
                tessdataPath = "C:\\Program Files\\Tesseract-OCR\\tessdata";
                libPath = "C:\\Program Files\\Tesseract-OCR";
                System.setProperty("jna.library.path", libPath);
                log.info("✅ Detected Windows — tessdata={}, lib={}", tessdataPath, libPath);
            } else {
                tessdataPath = "/usr/share/tesseract-ocr/tessdata";
                log.info("⚙️ Unknown OS detected. Using default tessdata path {}", tessdataPath);
            }

            // ✅ Приоритет: конфигурация из application.yml
            if (ocrProperties.getDatapath() != null && !ocrProperties.getDatapath().isBlank()) {
                tessdataPath = ocrProperties.getDatapath();
                log.info("Overriding tessdata path from configuration: {}", tessdataPath);
            }

            // ✅ Проверка существования и fallback
            if (!Files.isDirectory(Path.of(tessdataPath))) {
                log.warn("⚠️ Tessdata directory not found: {} — trying fallback search", tessdataPath);
                Path[] fallbackCandidates = {
                        Path.of("/opt/homebrew/share/tessdata"),
                        Path.of("/usr/local/share/tessdata"),
                        Path.of("/usr/share/tesseract-ocr/5/tessdata"),
                        Path.of("/usr/share/tesseract-ocr/tessdata")
                };
                tessdataPath = Stream.of(fallbackCandidates)
                        .filter(Files::isDirectory)
                        .map(Path::toString)
                        .findFirst()
                        .orElse(tessdataPath);
                log.info("🔍 Fallback tessdata path resolved to {}", tessdataPath);
            }

            System.setProperty("TESSDATA_PREFIX", tessdataPath);
            log.info("OCR initialized: path={}, languages={}, docker={}, lib={}",
                    tessdataPath, ocrProperties.getLanguages(), isContainer, libPath);

            // --- Глобальная установка переменной окружения для native библиотеки ---
            if (tessdataPath != null && !tessdataPath.isBlank()) {
                System.setProperty("TESSDATA_PREFIX", tessdataPath);
                try {
                    // 🪄 На macOS переменные среды могут быть изолированы — пробрасываем вручную
                    java.lang.reflect.Field f = System.class.getDeclaredField("props");
                    f.setAccessible(true);
                    Properties props = (Properties) f.get(null);
                    props.setProperty("TESSDATA_PREFIX", tessdataPath);
                } catch (Exception ignored) {
                }
                log.info("🔧 Set system TESSDATA_PREFIX to {}", tessdataPath);
            }


            ImageIO.scanForPlugins();
            log.info("✅ ImageIO plugins scanned and registered (JPEG/TIFF via TwelveMonkeys)");

        } catch (Exception e) {
            log.error("❌ OCR initialization failed: {}", e.getMessage(), e);
        }
    }

    @PostConstruct
    public void testImageReaders() {
        log.info("🧩 ImageIO readers available: {}", String.join(", ", ImageIO.getReaderFormatNames()));
    }

    // =============================================================
    // 🔍 OCR-извлечение с ThreadLocal
    // =============================================================
    private String extractWithOCR(File file) throws TesseractException {
        if (!ocrProperties.isEnabled()) {
            log.debug("🔕 OCR disabled. Skipping OCR extraction for {}", file.getName());
            return "";
        }
        log.debug("🧠 Running OCR (ThreadLocal) for {}", file.getName());
        Tesseract t = getTesseract();
        // гарантируем актуальные параметры
        t.setLanguage(ocrProperties.getLanguages());
        t.setPageSegMode(ocrProperties.getPsm());
        t.setOcrEngineMode(ocrProperties.getOem());
        return t.doOCR(file);
    }


    /**
     * Извлечение текста из файла и обновление search_text_cache.
     */
    @Async
    @Transactional
    public void indexObjectFile(ObjectFileEntity fileEntity) {
        Long versionId = fileEntity.getVersion() != null ? fileEntity.getVersion().getId() : null;
        if (versionId == null) {
            log.warn("File {} has no linked version. Skipping indexing.", fileEntity.getId());
            return;
        }

        File localFile = getLocalFile(fileEntity);
        if (localFile == null || !localFile.exists()) {
            log.warn("File for {} not found or cannot be accessed", fileEntity.getId());
            return;
        }

        try {
            String mime = detectMimeType(localFile);
            boolean imageLike = isImageLike(mime, fileEntity.getFileName());
            boolean pdfLike = isPdfLike(mime, fileEntity.getFileName());

            String tikaText = "";
            if (!imageLike && (ocrProperties.isCombineWithTika() || !pdfLike)) {
                tikaText = sanitizeText(parseToString(localFile));
            }

            boolean fallbackTriggered = isWeakText(tikaText);
            boolean runOcr = false;
            if (ocrProperties.isEnabled()) {
                if (imageLike) {
                    runOcr = true;
                } else if (pdfLike) {
                    runOcr = ocrProperties.isCombineWithTika() || fallbackTriggered;
                } else if (fallbackTriggered && isOcrFriendlyExtension(fileEntity.getFileName())) {
                    runOcr = true;
                }
            }


            if (pdfLike) {
                log.debug("Skipping OCR for PDF (safety mode) – handled by Tika only: {}", fileEntity.getFileName());
                runOcr = false;
            }


            String ocrText = "";
            if (runOcr) {
                if (imageLike) {
                    log.debug("🧠 Using OCR for image-like file {} (mime={})", fileEntity.getFileName(), mime);
                } else if (pdfLike && ocrProperties.isCombineWithTika() && !fallbackTriggered) {
                    log.debug("🧠 Combining Tika + OCR for PDF {} (mime={})", fileEntity.getFileName(), mime);
                } else if (fallbackTriggered) {
                    log.debug("🛟 Fallback OCR for {} (mime={}, extracted chars={})", fileEntity.getFileName(), mime,
                            meaningfulLength(tikaText));
                }

                ocrText = sanitizeText(extractWithOCR(localFile));
            }

            String text = mergeTexts(tikaText, ocrText).trim();

            if (text.isBlank()) {
                log.warn("No text extracted for version_id={}", versionId);
                return;
            }

            log.debug("📊 Extraction stats for {} -> tika={} chars, ocr={} chars, final={}",
                    fileEntity.getFileName(), meaningfulLength(tikaText), meaningfulLength(ocrText), meaningfulLength(text));

            // СОЗДАЁМ КЭШ ВСЕГДА
            SearchTextCache cache = new SearchTextCache();
            cache.setObjectVersionId(versionId);
            cache.setExtractedTextRaw(text);

            // ЯЗЫК — ОПЦИОНАЛЬНО
            if (languageDetectProperties.isEnabled()) {
                LanguageResult languageResult = detectLanguage(text);
                if (languageResult != null && !languageResult.isUnknown()) {
                    cache.setDetectedLanguage(languageResult.getLanguage());
                    cache.setLanguageConfidence(Double.valueOf(languageResult.getRawScore()));
                    log.debug("Detected language: {} (confidence: {})",
                            languageResult.getLanguage(), languageResult.getRawScore());
                } else {
                    log.debug("Language detection: unknown or failed");
                }
            } else {
                log.debug("Language detection disabled — skipping");
            }

            cacheRepository.save(cache);
            log.info("Indexed version_id={} [{} chars, lang={}]",
                    versionId, text.length(),
                    cache.getDetectedLanguage() != null ? cache.getDetectedLanguage() : "none");

        } catch (NoSuchMethodError e) {
            log.error("Tika runtime dependencies are misaligned: {}. Skipping indexing for file {}.",
                    e.getMessage(), fileEntity.getId(), e);
        } catch (IOException | TikaException | TesseractException e) {
            log.error("Failed to extract text for file {}: {}", fileEntity.getId(), e.getMessage(), e);
        } finally {
            if (fileEntity.isInline() && localFile != null) {
                try {
                    Files.deleteIfExists(localFile.toPath());
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Возвращает локальный файл для чтения (inline или внешний).
     */
    private File getLocalFile(ObjectFileEntity fileEntity) {
        try {
            if (fileEntity.isInline() && fileEntity.getContent() != null) {
                // Определяем расширение из имени файла, если есть
                String ext = "jpg";
                String originalName = fileEntity.getFileName();
                if (originalName != null && originalName.contains(".")) {
                    ext = originalName.substring(originalName.lastIndexOf('.') + 1);
                }

                // Создаём временный файл с корректным расширением
                File tmp = File.createTempFile("anubis-inline-", "." + ext);
                Files.write(tmp.toPath(), fileEntity.getContent());
                return tmp;
            } else if (fileEntity.getExternalFilePath() != null) {
                return new File(fileEntity.getExternalFilePath());
            }
        } catch (IOException e) {
            log.error("⚠️ Cannot create local copy for file {}: {}", fileEntity.getId(), e.getMessage());
        }
        return null;
    }


    private String extractWithOCR(InputStream inputStream, String originalFileName)
            throws IOException, TesseractException {
        String ext = originalFileName != null && originalFileName.contains(".")
                ? originalFileName.substring(originalFileName.lastIndexOf('.') + 1)
                : "jpg";

        Path tempFile = Files.createTempFile("ocr-", "." + ext);
        try (OutputStream out = Files.newOutputStream(tempFile)) {
            inputStream.transferTo(out);
        }

        try {
            log.debug("🧠 Running OCR (ThreadLocal) for {} (temp file: {})", originalFileName, tempFile);
            Tesseract t = getTesseract();
            t.setLanguage(ocrProperties.getLanguages());
            t.setPageSegMode(ocrProperties.getPsm());
            t.setOcrEngineMode(ocrProperties.getOem());
            return t.doOCR(tempFile.toFile());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }



    private String sanitizeText(String text) {
        if (text == null) {
            return "";
        }
        String sanitized = text.replace('\u0000', ' ');
        sanitized = sanitized.replace("\r\n", "\n");
        sanitized = sanitized.replace('\r', '\n');
        return sanitized.trim();
    }

    private String mergeTexts(String tikaText, String ocrText) {
        String primary = tikaText == null ? "" : tikaText;
        String secondary = ocrText == null ? "" : ocrText;

        if (primary.isBlank()) {
            return secondary;
        }
        if (secondary.isBlank()) {
            return primary;
        }
        if (primary.equalsIgnoreCase(secondary) || primary.contains(secondary)) {
            return primary;
        }
        if (secondary.contains(primary)) {
            return secondary;
        }
        return primary + System.lineSeparator() + System.lineSeparator() + secondary;
    }

    private int meaningfulLength(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return (int) text.codePoints()
                .filter(ch -> !Character.isWhitespace(ch))
                .count();
    }

    private boolean isWeakText(String text) {
        if (!ocrProperties.isFallbackEnabled()) {
            return false;
        }
        int threshold = ocrProperties.getFallbackMinLength();
        if (threshold <= 0) {
            return false;
        }
        return meaningfulLength(text) < threshold;
    }

    private boolean isImageLike(String mime, String fileName) {
        if (mime != null && mime.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return true;
        }
        return IMAGE_EXTENSIONS.contains(extensionOf(fileName));
    }

    private boolean isPdfLike(String mime, String fileName) {
        if (mime != null && "application/pdf".equalsIgnoreCase(mime)) {
            return true;
        }
        return PDF_EXTENSIONS.contains(extensionOf(fileName));
    }

    private boolean isOcrFriendlyExtension(String fileName) {
        String ext = extensionOf(fileName);
        return IMAGE_EXTENSIONS.contains(ext) || PDF_EXTENSIONS.contains(ext);
    }

    private String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }


    private LanguageResult detectLanguage(String text) {
        if (!languageDetectProperties.isEnabled() || languageDetector == null || text == null || text.isBlank()) {
            return null;
        }

        try {
            return languageDetector.detect(text);
        } catch (Exception e) {
            log.warn("Language detection failed: {}", e.getMessage());
            return null;
        }
    }

    private String detectMimeType(File file) throws IOException {
        try {
            return tika.detect(file);
        } catch (NoSuchMethodError e) {
            log.warn("Tika detect failed due to missing SystemProperties#getUserName: {}. Falling back to NIO probe for {}.",
                    e.getMessage(), file.getName());
            try {
                return Files.probeContentType(file.toPath());
            } catch (IOException ioException) {
                log.warn("Fallback MIME detection failed for {}: {}", file.getName(), ioException.getMessage());
                throw ioException;
            }
        }
    }

    /**
     * Извлечение текста через Apache Tika без ограничения 100 000 символов.
     * Использует BodyContentHandler(-1), чтобы сохранить весь текст.
     */
    private String parseToString(File file) throws IOException, TikaException {
        try (InputStream stream = new FileInputStream(file)) {
            org.apache.tika.parser.AutoDetectParser parser =
                    new org.apache.tika.parser.AutoDetectParser();
            org.apache.tika.metadata.Metadata metadata = new org.apache.tika.metadata.Metadata();

            // ❗ отключаем ограничение длины текста (-1 = без лимита)
            org.apache.tika.sax.BodyContentHandler handler =
                    new org.apache.tika.sax.BodyContentHandler(-1);

            org.apache.tika.parser.ParseContext context =
                    new org.apache.tika.parser.ParseContext();
            context.set(org.apache.tika.parser.AutoDetectParser.class, parser);

            parser.parse(stream, handler, metadata, context);
            return handler.toString();
        } catch (NoSuchMethodError e) {
            log.error("Tika parse failed due to missing SystemProperties#getUserName: {}. Returning empty text for {}.",
                    e.getMessage(), file.getName());
            return "";
        } catch (Exception e) {
            log.error("Tika parse error for {}: {}", file.getName(), e.getMessage(), e);
            return "";
        }
    }



    /**
     * Массовая переиндексация.
     */
    @Async
    @Transactional
    public void reindexAll() {
        List<ObjectFileEntity> files = fileRepository.findAll();
        files.forEach(this::indexObjectFile);
    }

    @Async
    @Transactional
    public void reindexOcrCandidates() {
        List<ObjectFileEntity> candidates = fileRepository.findAllOcrCandidates();
        if (candidates == null || candidates.isEmpty()) {
            log.info("🔄 OCR reindex requested, but no candidates were found.");
            return;
        }

        log.info("🔄 Reindexing {} OCR candidates...", candidates.size());
        Set<Long> processed = new HashSet<>();
        for (ObjectFileEntity file : candidates) {
            Long versionId = file.getVersion() != null ? file.getVersion().getId() : null;
            if (versionId == null) {
                log.debug("Skipping file {} without version during OCR reindex", file.getId());
                continue;
            }
            if (processed.add(versionId)) {
                indexObjectFile(file);
            }
        }
        log.info("✅ OCR-focused reindex finished ({} versions processed)", processed.size());
    }

    @Async
    @Transactional
    public void indexMissing() {
        List<ObjectFileEntity> files = fileRepository.findAllWithoutIndexedText();
        if (files == null || files.isEmpty()) {
            log.info("🔍 No missing search_text_cache entries detected.");
            return;
        }

        log.info("🔍 Indexing {} versions that have no cached text...", files.size());
        Set<Long> processed = new HashSet<>();
        for (ObjectFileEntity file : files) {
            Long versionId = file.getVersion() != null ? file.getVersion().getId() : null;
            if (versionId == null) {
                continue;
            }
            if (processed.add(versionId)) {
                indexObjectFile(file);
            }
        }
        log.info("✅ Indexed {} previously missing versions", processed.size());
    }

    /**
     * Поиск по контенту.
     */
    @Transactional(readOnly = true)
    public List<Long> search(String query) {
        return cacheRepository.searchByText(query);
    }

    /**
     * Выполняет полнотекстовый поиск в search_text_cache и возвращает
     * список version_id, где найдено совпадение.
     */
    @Transactional(readOnly = true)
    public Set<Long> findMatchingVersionIds(String queryText) {
        if (queryText == null || queryText.isBlank()) return Set.of();

        try {
            // Определяем тип tsquery
            String tsFunction = "plainto_tsquery";
            if (queryText.contains("\"") || queryText.contains("+") || queryText.contains("-")) {
                tsFunction = "websearch_to_tsquery";
            }

            // Можно указать расширенную конфигурацию словаря
            // Например: multilang = unaccent | english | russian | georgian
            String config = "multilang";

            String sql = """
                    SELECT object_version_id
                    FROM search_text_cache
                    WHERE extracted_text_vector @@ %s(:config::regconfig, :query)
                    """.formatted(tsFunction);

            Query q = em.createNativeQuery(sql);
            q.setParameter("config", config);
            q.setParameter("query", queryText);

            @SuppressWarnings("unchecked")
            var results = q.getResultList();

            Set<Long> ids = new HashSet<>();
            for (Object r : results) {
                if (r instanceof Number n) ids.add(n.longValue());
            }

            log.debug("🔍 FTS [{}:{}] '{}' -> {} results",
                    tsFunction, config, queryText, ids.size());
            return ids;

        } catch (Exception e) {
            log.error("❌ Ошибка FTS-запроса для '{}': {}", queryText, e.getMessage());
            return Set.of();
        }
    }

    /**
     * Переиндексация всех файлов конкретной версии объекта.
     */
    @Async
    @Transactional
    public void reindexSingle(Long versionId) {
        if (versionId == null) {
            log.warn("⚠️ reindexSingle called with null versionId");
            return;
        }

        var files = fileRepository.findByVersion_Id(versionId);
        if (files == null || files.isEmpty()) {
            log.warn("⚠️ No files found for versionId={}", versionId);
            return;
        }

        log.info("♻️ Reindexing version {} ({} files)...", versionId, files.size());
        files.forEach(this::indexObjectFile);
        log.info("✅ Completed reindex for version {}", versionId);
    }
}