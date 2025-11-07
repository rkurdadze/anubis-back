package ge.comcom.anubis.controller.core;

import ge.comcom.anubis.service.core.FullTextSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Slf4j
public class FullTextSearchController {

    private final FullTextSearchService searchService;

    /**
     * Поиск по содержимому (OCR + текстовые документы).
     *
     * Пример:
     *   GET /api/search?q=договор
     */
    @GetMapping
    public ResponseEntity<Set<Long>> search(@RequestParam("q") String query) {
        log.info("🔍 Search request: '{}'", query);
        Set<Long> results = searchService.findMatchingVersionIds(query);
        return ResponseEntity.ok(results);
    }

    /**
     * Запуск полной переиндексации (асинхронно).
     *
     * Пример:
     *   POST /api/search/reindex
     */
    @PostMapping("/reindex")
    public ResponseEntity<String> reindexAll() {
        log.info("♻️ Starting full reindex...");
        searchService.reindexAll();
        return ResponseEntity.accepted().body("Reindexing started");
    }

    @PostMapping("/reindex/ocr")
    public ResponseEntity<String> reindexOcrCandidates() {
        log.info("♻️ Starting OCR-focused reindex...");
        searchService.reindexOcrCandidates();
        return ResponseEntity.accepted().body("OCR reindex started");
    }

    @PostMapping("/reindex/missing")
    public ResponseEntity<String> indexMissing() {
        log.info("♻️ Indexing versions without cached text...");
        searchService.indexMissing();
        return ResponseEntity.accepted().body("Indexing of missing entries started");
    }

    /**
     * Переиндексация конкретного объекта по его версии.
     *
     * Пример:
     *   POST /api/search/reindex/42
     */
    @PostMapping("/reindex/{versionId}")
    public ResponseEntity<String> reindexSingle(@PathVariable Long versionId) {
        log.info("♻️ Reindexing version {}", versionId);
        searchService.reindexSingle(versionId);
        return ResponseEntity.accepted()
                .body("Reindex for version " + versionId + " started");
    }
}