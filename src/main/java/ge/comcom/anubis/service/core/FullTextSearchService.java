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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    private final ObjectFileRepository fileRepository;
    private final SearchTextCacheRepository cacheRepository;
    private final OcrProperties ocrProperties;
    private final LanguageDetectProperties languageDetectProperties;

    private final Tika tika = new Tika();
    private Tesseract ocr;
    private LanguageDetector languageDetector;

    @PersistenceContext
    private EntityManager em;

    @PostConstruct
    private void initEngines() {
        // 🔹 Language detection
        if (languageDetectProperties.isEnabled()) {
            try {
                languageDetector = new OptimaizeLangDetector().loadModels();
                log.info("Language detector initialized (Optimaize)");
            } catch (Exception e) {
                languageDetector = null;
                log.warn("Language detector failed to initialize: {}. Skipping.", e.getMessage());
            }
        } else {
            languageDetector = null;
            log.info("Language detection disabled in configuration.");
        }

        // 🔹 OCR
        if (!ocrProperties.isEnabled()) {
            log.info("OCR disabled in configuration.");
            return;
        }

        try {
            String os = System.getProperty("os.name");
            String osLower = os.toLowerCase();
            boolean isContainer = Files.exists(Path.of("/.dockerenv"));
            String tessdataPath;
            String libPath = null;

            log.info("Detected operating system: {}", os);

            // --- macOS ---
            if (osLower.contains("mac") || osLower.contains("darwin")) {
                Path[] dataCandidates = {
                        Path.of("/usr/local/share/tessdata"), // Intel mac
                        Path.of("/opt/homebrew/share/tessdata"), // M1/M2 mac
                        Path.of("/usr/share/tessdata")
                };
                tessdataPath = Stream.of(dataCandidates)
                        .filter(Files::exists)
                        .map(Path::toString)
                        .findFirst()
                        .orElse("/usr/local/share/tessdata");

                Path[] libCandidates = {
                        Path.of("/usr/local/lib/libtesseract.dylib"), // Intel mac
                        Path.of("/opt/homebrew/lib/libtesseract.dylib"), // M1 mac
                        Path.of("/usr/local/Cellar/tesseract/5.5.1/lib/libtesseract.dylib")
                };
                libPath = Stream.of(libCandidates)
                        .filter(Files::exists)
                        .map(path -> path.getParent().toString())
                        .findFirst()
                        .orElse(null);

                log.info("✅ Detected macOS — using tessdata={} and library={}", tessdataPath, libPath);

                if (libPath != null) {
                    System.setProperty("jna.library.path", libPath);
                    System.setProperty("DYLD_LIBRARY_PATH", libPath);
                }
                System.setProperty("TESSDATA_PREFIX", tessdataPath);
            }

            // --- Linux ---
            else if (osLower.contains("linux")) {
                tessdataPath = "/usr/share/tesseract-ocr/5/tessdata";
                libPath = "/usr/lib/x86_64-linux-gnu";
                System.setProperty("TESSDATA_PREFIX", tessdataPath);
                System.setProperty("LD_LIBRARY_PATH", libPath);
                System.setProperty("jna.library.path", libPath);
                log.info("✅ Detected Linux — tessdata={}, lib={}", tessdataPath, libPath);
            }

            // --- Windows ---
            else if (osLower.contains("win")) {
                tessdataPath = "C:\\Program Files\\Tesseract-OCR\\tessdata";
                libPath = "C:\\Program Files\\Tesseract-OCR";
                System.setProperty("jna.library.path", libPath);
                System.setProperty("TESSDATA_PREFIX", tessdataPath);
                log.info("✅ Detected Windows — tessdata={}, lib={}", tessdataPath, libPath);
            }

            // --- Fallback ---
            else {
                tessdataPath = "/usr/share/tesseract-ocr/tessdata";
                System.setProperty("TESSDATA_PREFIX", tessdataPath);
                log.info("Using fallback TESSDATA_PREFIX = {}", tessdataPath);
            }

            // --- Создаём и настраиваем Tesseract ---
            ocr = new Tesseract();
            ocr.setDatapath(tessdataPath);
            ocr.setLanguage(ocrProperties.getLanguages());
            ocr.setPageSegMode(ocrProperties.getPsm());
            ocr.setOcrEngineMode(ocrProperties.getOem());

            // --- Проверяем языковые файлы ---
            String[] langs = ocrProperties.getLanguages().split("\\+");
            for (String lang : langs) {
                Path langFile = Path.of(tessdataPath, lang.trim() + ".traineddata");
                if (!Files.exists(langFile)) {
                    log.warn("⚠️ Missing traineddata for '{}'. Try: brew install tesseract-lang", lang);
                } else {
                    log.debug("✅ Found traineddata for '{}'", lang);
                }
            }

            log.info("OCR initialized: path={}, languages={}, os={}, docker={}, lib={}",
                    tessdataPath, ocrProperties.getLanguages(), os, isContainer, libPath);

        } catch (Exception e) {
            log.error("❌ OCR initialization failed: {}", e.getMessage(), e);
            ocr = null;
        }
    }




    @PostConstruct
    public void testImageReaders() {
        String[] readers = ImageIO.getReaderFormatNames();
        log.info("🧩 ImageIO readers available: {}", String.join(", ", readers));
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
            String text;

            if (mime != null && (mime.startsWith("image/") || mime.equals("application/pdf"))) {
                if (fileEntity.isInline() && fileEntity.getContent() != null) {
                    try (InputStream in = new ByteArrayInputStream(fileEntity.getContent())) {
                        text = extractWithOCR(in, fileEntity.getFileName());
                    }
                } else {
                    text = extractWithOCR(localFile);
                }
            } else {
                text = parseToString(localFile);
            }


            if (text == null || text.isBlank()) {
                log.warn("No text extracted for version_id={}", versionId);
                return;
            }

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


    /**
     * OCR-извлечение текста.
     */
    private String extractWithOCR(File file) throws TesseractException {
        if (!ocrProperties.isEnabled()) {
            log.debug("🔕 OCR disabled. Skipping OCR extraction for {}", file.getName());
            return "";
        }

        if (ocr == null) return "";
        log.debug("🧠 Running OCR for {}", file.getName());
        return ocr.doOCR(file);
    }

    private String extractWithOCR(InputStream inputStream, String originalFileName) throws IOException, TesseractException {
        String ext = originalFileName != null && originalFileName.contains(".")
                ? originalFileName.substring(originalFileName.lastIndexOf('.') + 1)
                : "jpg";

        Path tempFile = Files.createTempFile("ocr-", "." + ext);
        try (OutputStream out = Files.newOutputStream(tempFile)) {
            inputStream.transferTo(out);
        }

        try {
            if (ocr == null) {
                log.warn("⚠️ OCR is not initialized, skipping OCR for {}", originalFileName);
                return "";
            }

            log.debug("🧠 Running OCR for {} (temp file: {})", originalFileName, tempFile);
            return ocr.doOCR(tempFile.toFile());
        } finally {
            Files.deleteIfExists(tempFile);
        }
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

    private String parseToString(File file) throws IOException, TikaException {
        try {
            return tika.parseToString(file);
        } catch (NoSuchMethodError e) {
            log.error("Tika parse failed due to missing SystemProperties#getUserName: {}. Returning empty text for {}.",
                    e.getMessage(), file.getName());
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