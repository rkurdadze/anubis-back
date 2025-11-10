package ge.comcom.anubis.service.core;

import ge.comcom.anubis.config.LanguageDetectProperties;
import ge.comcom.anubis.dto.ws.FileStatusMessage;
import ge.comcom.anubis.entity.core.ObjectFileEntity;
import ge.comcom.anubis.entity.core.SearchTextCache;
import ge.comcom.anubis.integration.ocr.RemoteOcrClient;
import ge.comcom.anubis.integration.ocr.RemoteOcrResponse;
import ge.comcom.anubis.repository.core.ObjectFileRepository;
import ge.comcom.anubis.repository.core.SearchTextCacheRepository;
import ge.comcom.anubis.service.SocketNotifierService;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class FullTextSearchService {

    private final ObjectFileRepository fileRepository;
    private final SearchTextCacheRepository cacheRepository;
    private final RemoteOcrClient remoteOcrClient;
    private final LanguageDetectProperties languageDetectProperties;
    private final SocketNotifierService socketNotifierService;

    private LanguageDetector languageDetector;

    @PersistenceContext
    private EntityManager em;

    @PostConstruct
    void initLanguageDetector() {
        if (!languageDetectProperties.isEnabled()) {
            log.info("Language detection disabled in configuration");
            return;
        }
        try {
            languageDetector = new OptimaizeLangDetector().loadModels();
            log.info("Language detector initialized (Optimaize)");
        } catch (Exception e) {
            languageDetector = null;
            log.warn("Language detector failed to initialize: {}", e.getMessage());
        }
    }

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
            Optional<RemoteOcrResponse> responseOptional = remoteOcrClient.extract(localFile, fileEntity.getFileName());
            if (responseOptional.isEmpty()) {
                log.warn("Remote OCR returned empty result for file {}", fileEntity.getId());
                return;
            }

            RemoteOcrResponse response = responseOptional.get();
            String tikaText = sanitizeText(response.tikaText());
            String ocrText = sanitizeText(response.ocrText());
            String combined = sanitizeText(response.combinedText());

            if (combined.isBlank()) {
                combined = mergeTexts(tikaText, ocrText).trim();
            }

            if (combined.isBlank()) {
                log.warn("No text extracted for version_id={}", versionId);
                return;
            }

            SearchTextCache cache = new SearchTextCache();
            cache.setObjectVersionId(versionId);
            cache.setExtractedTextRaw(combined);

            if (languageDetectProperties.isEnabled()) {
                LanguageResult languageResult = detectLanguage(combined);
                if (languageResult != null && !languageResult.isUnknown()) {
                    cache.setDetectedLanguage(languageResult.getLanguage());
                    cache.setLanguageConfidence(Double.valueOf(languageResult.getRawScore()));
                    log.debug("Detected language: {} (confidence: {})", languageResult.getLanguage(), languageResult.getRawScore());
                } else {
                    log.debug("Language detection: unknown or failed");
                }
            }

            cacheRepository.save(cache);
            log.info("Indexed version_id={} [combined={} chars, tika={}, ocr={}]", versionId,
                    combined.length(), meaningfulLength(tikaText), meaningfulLength(ocrText));
            notifyFileIndexed(fileEntity.getId(), versionId, true, null);
        } catch (Exception e) {
            String errorMessage = String.format(
                    "Failed to extract text for file %d: %s",
                    fileEntity.getId(),
                    e.getMessage()
            );
            log.error(errorMessage, e);
            notifyFileIndexed(fileEntity.getId(), versionId, false, errorMessage);
        } finally {
            if (fileEntity.isInline() && localFile != null) {
                try {
                    Files.deleteIfExists(localFile.toPath());
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void notifyFileIndexed(Long fileId, Long versionId, boolean success, String errorMsg) {
        String status = success ? "INDEXED" : "FAILED";
        FileStatusMessage payload = new FileStatusMessage(fileId, versionId, status, errorMsg);
//        socketNotifierService.toFileVersion(versionId, "FILE_STATUS", payload);
        socketNotifierService.toAllFiles("FILE_STATUS", payload);
    }

    private File getLocalFile(ObjectFileEntity fileEntity) {
        try {
            if (fileEntity.isInline() && fileEntity.getContent() != null) {
                String ext = extensionOf(fileEntity.getFileName());
                if (ext.isBlank()) {
                    ext = "bin";
                }
                File tmp = File.createTempFile("anubis-inline-", "." + ext);
                try (OutputStream out = Files.newOutputStream(tmp.toPath())) {
                    out.write(fileEntity.getContent());
                }
                return tmp;
            } else if (fileEntity.getExternalFilePath() != null) {
                return new File(fileEntity.getExternalFilePath());
            }
        } catch (IOException e) {
            log.error("Cannot create local copy for file {}: {}", fileEntity.getId(), e.getMessage());
        }
        return null;
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

    private String mergeTexts(String primary, String secondary) {
        String a = primary == null ? "" : primary;
        String b = secondary == null ? "" : secondary;

        if (a.isBlank()) {
            return b;
        }
        if (b.isBlank()) {
            return a;
        }
        if (a.equalsIgnoreCase(b) || a.contains(b)) {
            return a;
        }
        if (b.contains(a)) {
            return b;
        }
        return (a + "\n\n" + b).trim();
    }

    private int meaningfulLength(String text) {
        if (text == null) {
            return 0;
        }
        return (int) text.chars().filter(ch -> !Character.isWhitespace(ch)).count();
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
            log.info("OCR reindex requested, but no candidates were found.");
            return;
        }

        log.info("Reindexing {} OCR candidates...", candidates.size());
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
        log.info("OCR-focused reindex finished ({} versions processed)", processed.size());
    }

    @Async
    @Transactional
    public void indexMissing() {
        List<ObjectFileEntity> files = fileRepository.findAllWithoutIndexedText();
        if (files == null || files.isEmpty()) {
            log.info("No missing search_text_cache entries detected.");
            return;
        }

        log.info("Indexing {} versions that have no cached text...", files.size());
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
        log.info("Indexed {} previously missing versions", processed.size());
    }

    @Transactional(readOnly = true)
    public List<Long> search(String query) {
        return cacheRepository.searchByText(query);
    }

    @Transactional(readOnly = true)
    public Set<Long> findMatchingVersionIds(String queryText) {
        if (queryText == null || queryText.isBlank()) return Set.of();

        try {
            String tsFunction = "plainto_tsquery";
            if (queryText.contains("\"") || queryText.contains("+") || queryText.contains("-")) {
                tsFunction = "websearch_to_tsquery";
            }

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

            log.debug("FTS [{}:{}] '{}' -> {} results", tsFunction, config, queryText, ids.size());
            return ids;

        } catch (Exception e) {
            log.error("Ошибка FTS-запроса для '{}': {}", queryText, e.getMessage());
            return Set.of();
        }
    }

    @Async
    @Transactional
    public void reindexSingle(Long versionId) {
        if (versionId == null) {
            log.warn("reindexSingle called with null versionId");
            return;
        }

        var files = fileRepository.findByVersion_Id(versionId);
        if (files == null || files.isEmpty()) {
            log.warn("No files found for versionId={}", versionId);
            return;
        }

        log.info("Reindexing version {} ({} files)...", versionId, files.size());
        files.forEach(this::indexObjectFile);
        log.info("Completed reindex for version {}", versionId);
    }
}
