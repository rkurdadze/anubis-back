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

import java.io.*;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Реализация полнотекстового поиска для Anubis.
 * Поддерживает:
 *  - Apache Tika для текстовых форматов (PDF, DOCX, XLSX, HTML и т.д.)
 *  - Tesseract OCR для изображений и сканов
 *  - Inline (BYTEA) и внешние файлы (FS/S3)
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
        try {
            languageDetector = new OptimaizeLangDetector().loadModels();
            log.info("🗣️ Language detector initialized (Optimaize)");
        } catch (Exception e) { // можно оставить общий Exception
            languageDetector = null;
            log.warn("⚠️ Language detector disabled: {}", e.getMessage());
        }

        if (!ocrProperties.isEnabled()) {
            log.info("🧩 OCR disabled in configuration.");
            return;
        }

        ocr = new Tesseract();
        ocr.setDatapath(ocrProperties.getDatapath());
        ocr.setLanguage(ocrProperties.getLanguages());
        ocr.setPageSegMode(ocrProperties.getPsm());
        ocr.setOcrEngineMode(ocrProperties.getOem());

        log.info("🧠 OCR initialized: path={}, languages={}",
                ocrProperties.getDatapath(), ocrProperties.getLanguages());
    }


    /**
     * Извлечение текста из файла и обновление search_text_cache.
     */
    @Async
    @Transactional
    public void indexObjectFile(ObjectFileEntity fileEntity) {
        if (!languageDetectProperties.isEnabled()) {
            log.debug("🔕 Language detection disabled. Skipping indexing for file {}", fileEntity.getId());
            return;
        }

        Long versionId = fileEntity.getVersion() != null ? fileEntity.getVersion().getId() : null;
        if (versionId == null) {
            log.warn("⚠️ File {} has no linked version. Skipping indexing.", fileEntity.getId());
            return;
        }

        File localFile = getLocalFile(fileEntity);
        if (localFile == null || !localFile.exists()) {
            log.warn("⚠️ File for {} not found or cannot be accessed", fileEntity.getId());
            return;
        }

        try {
            String mime = tika.detect(localFile);
            String text;

            if (mime.startsWith("image/") || mime.equals("application/pdf")) {
                // OCR для изображений и PDF-сканов
                text = extractWithOCR(localFile);
            } else {
                // Текстовое извлечение Tika
                text = tika.parseToString(localFile);
            }

            if (text != null && !text.isBlank()) {
                SearchTextCache cache = new SearchTextCache();
                cache.setObjectVersionId(versionId);
                cache.setExtractedTextRaw(text); // ✅ обновлённое имя поля

                LanguageResult languageResult = detectLanguage(text);
                if (languageResult != null && !languageResult.isUnknown()) {
                    cache.setDetectedLanguage(languageResult.getLanguage());
                    cache.setLanguageConfidence(Double.valueOf(languageResult.getRawScore()));
                }

                cacheRepository.save(cache);
                log.info("✅ Indexed version_id={} [{} chars]", versionId, text.length());
            } else {
                log.warn("⚠️ No text extracted for version_id={}", versionId);
            }

        } catch (IOException | TikaException | TesseractException e) {
            log.error("❌ Failed to extract text for file {}: {}", fileEntity.getId(), e.getMessage());
        } finally {
            if (fileEntity.isInline()) {
                try {
                    Files.deleteIfExists(localFile.toPath());
                } catch (IOException ignored) {}
            }
        }
    }

    /**
     * Возвращает локальный файл для чтения (inline или внешний).
     */
    private File getLocalFile(ObjectFileEntity fileEntity) {
        try {
            if (fileEntity.isInline() && fileEntity.getContent() != null) {
                File tmp = File.createTempFile("anubis-inline-", ".bin");
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

    private LanguageResult detectLanguage(String text) {
        if (languageDetector == null || text == null || text.isBlank()) {
            return null;
        }

        try {
            return languageDetector.detect(text); // без .copy()
        } catch (Exception e) {
            log.warn("⚠️ Language detection failed: {}", e.getMessage());
            return null;
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