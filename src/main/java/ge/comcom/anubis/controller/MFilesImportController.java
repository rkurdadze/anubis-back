package ge.comcom.anubis.controller;

import ge.comcom.anubis.service.MFilesImportService;
import ge.comcom.anubis.service.MFilesImportService.ImportSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;

@Slf4j
@RestController
@RequestMapping("/api/v1/import/mfiles")
@RequiredArgsConstructor
public class MFilesImportController {

    private final MFilesImportService importService;

    /**
     * Импорт из M-Files backup (CSV + Files) в указанный Vault.
     * Пример вызова:
     * POST /api/v1/import/mfiles
     * {
     *   "backupPath": "C:\\Backups\\newbackup2910",
     *   "vaultId": 3
     * }
     */
    @PostMapping
    public ResponseEntity<ImportSummary> importBackup(@RequestBody ImportRequest req) {
        try {
            Path backupPath = Path.of(req.backupPath());
            Long vaultId = req.vaultId();
            log.info("▶️ Запуск импорта из: {} → vaultId={}", backupPath, vaultId);

            ImportSummary summary = importService.importBackup(backupPath, vaultId);
            log.info("✅ Импорт завершён: {} успешных, {} ошибок, {} отсутствующих файлов",
                    summary.success(), summary.failed(), summary.missingFiles());

            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            log.error("❌ Ошибка импорта: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ImportSummary(0, 0, 0, 0, java.util.List.of("Глобальная ошибка: " + e.getMessage())));
        }
    }

    public record ImportRequest(String backupPath, Long vaultId) {}
}