package ge.comcom.anubis.service;

import ge.comcom.anubis.dto.FileStorageRequest;
import ge.comcom.anubis.dto.ObjectDto;

import ge.comcom.anubis.entity.core.*;
import ge.comcom.anubis.entity.core.ValueList;
import ge.comcom.anubis.entity.core.ValueListItem;
import ge.comcom.anubis.entity.meta.PropertyDef;
import ge.comcom.anubis.enums.PropertyDataType;
import ge.comcom.anubis.enums.StorageKindEnum;
import ge.comcom.anubis.repository.meta.PropertyDefRepository;
import ge.comcom.anubis.service.core.FileService;
import ge.comcom.anubis.service.core.ObjectService;
import ge.comcom.anubis.service.core.ObjectTypeService;
import ge.comcom.anubis.service.meta.ClassService;
import ge.comcom.anubis.service.meta.PropertyDefService;
import ge.comcom.anubis.service.meta.ValueListService;
import ge.comcom.anubis.service.storage.FileStorageAdminService;
import ge.comcom.anubis.service.storage.VaultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.mock.web.MockMultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Импорт из M-Files backup (CSV + папка "(Files)").
 * - Создаёт недостающие ObjectType/Class/PropertyDef/ValueList/ValueListItem «на лету»
 * - Привязывает новые ObjectType к заранее существующему Vault (через код/ID)
 * - Импортирует файлы через FileService.saveFile(objectId, MultipartFile)
 * - Ведёт подробный лог в консоль и возвращает JSON-сводку
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MFilesImportService {

    private final ObjectTypeService objectTypeService;
    private final ClassService classService;
    private final PropertyDefService propertyDefService;
    private final PropertyDefRepository propertyDefRepository;
    private final ValueListService valueListService;
    private final ObjectService objectService;
    private final FileService fileService;
    private final VaultService vaultService;
    private final FileStorageAdminService fileStorageAdminService;

    /**
     * Код дефолтного vault, к которому будут привязываться новые ObjectType (vault уже существует в БД)
     */
    @Value("${anubis.import.default-vault-code:MAIN}")
    private String defaultVaultCode;

    private static final String FILE_COL = "File";
    private static final String OBJECT_TYPE_COL = "Object Type";
    private static final String CLASS_COL = "Class";

    // из вашего задания: валюлисты и мульти-валюлист
    private static final String DOC_TYPE = "დოკუმენტის ტიპი";
    private static final String DOC_SUBTYPE = "დოკუმენტის ქვეტიპი";
    private static final String ORDER_TYPE = "ბრძანების ტიპი";
    private static final String ADDITIONAL_CLASSES = "Additional classes";
    private static final String SINGLE_FILE = "Single file";
    private static final String DATE_CREATED = "Date Created";
    private static final String DATE_MODIFIED = "Date Modified";

    /**
     * Разделители для мультизначных колонок
     */
    private static final String MULTI_SPLIT_REGEX = "[;,]";

    /**
     * Колонка вида "Название [id<:ver>]"?
     */
    private boolean isIdVerColumn(String col) {
        if (col == null) return false;
        String s = col.replace("\uFEFF", "").trim().toLowerCase(Locale.ROOT);
        return s.endsWith("[id<:ver>]") || s.contains(" [id<:ver>]");
    }

    /**
     * Базовое имя без хвоста " [id<:ver>]"
     */
    private String baseNameFromIdVer(String col) {
        if (col == null) return "";
        String s = col.replace("\uFEFF", "").trim();
        int idx = s.lastIndexOf(" [id<:ver>]");
        return idx >= 0 ? s.substring(0, idx).trim() : s;
    }


    /**
     * Проверяет, есть ли у таблицы пара колонок вида "Имя" и "Имя [id<:ver>]"
     */
    private boolean hasIdVerPair(Map<String, Integer> headers, String baseName) {
        return headers.keySet().stream().anyMatch(h ->
                normalizeHeader(h).equals(normalizeHeader(baseName + " [id<:ver>]")));
    }

    /**
     * Возвращает значение из "id" колонки, если такая пара есть
     */
    private String findIdVerValue(CSVRecord row, Map<String, String> headerAlias, String baseName) {
        String idCol = baseName + " [id<:ver>]";
        return value(row, headerAlias, idCol);
    }

    public ImportSummary importBackup(Path backupRoot, Long vaultId) {
        VaultEntity vault = resolveVault(vaultId);
        log.info("🔐 Импорт в vault '{}' (id={})", vault.getName(), vault.getId());
        return importBackupInternal(backupRoot, vault);
    }

    /**
     * Импортирует backup из указанной директории в указанный vault.
     * - Ищет CSV и папку "(Files)"
     * - Парсит записи и вызывает importRow для каждой
     * - Собирает и возвращает ImportSummary
     */
    private ImportSummary importBackupInternal(Path backupRoot, VaultEntity vault) {
        ImportStats stats = new ImportStats();
        // DEBUG: Проверяем видимость пути backupRoot
        log.info("🔍 Проверка пути {} → существует={}, файл={}, каталог={}",
                backupRoot,
                Files.exists(backupRoot),
                Files.isRegularFile(backupRoot),
                Files.isDirectory(backupRoot));
        Path csvPath;
        Path filesDir = null;
        try {
            if (Files.isDirectory(backupRoot)) {
                csvPath = findCsv(backupRoot);
            } else if (Files.isRegularFile(backupRoot) && backupRoot.toString().toLowerCase().endsWith(".csv")) {
                csvPath = backupRoot;
            } else {
                throw new FileNotFoundException("Указанный путь не является CSV или каталогом: " + backupRoot);
            }
        } catch (IOException e) {
            log.error("❌ Ошибка поиска CSV: {}", e.getMessage());
            stats.errors.add("CSV не найден: " + e.getMessage());
            return new ImportSummary(0, 0, 0, 0, stats.errors);
        }
        filesDir = findFilesDir(backupRoot);
        log.info("📄 CSV: {}", csvPath);
        if (filesDir != null) {
            log.info("📁 Каталог файлов: {}", filesDir);
        }
        try (
                Reader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8);
                CSVParser parser = CSVFormat.DEFAULT
                        .withFirstRecordAsHeader()
                        .withTrim()
                        .parse(reader)
        ) {
            Map<String, Integer> headers = parser.getHeaderMap();
            // Нормализация заголовков и alias оригинальных ключей
            Map<String, String> headerAlias = new LinkedHashMap<>();
            for (String h : headers.keySet()) {
                String norm = normalizeHeader(h);
                headerAlias.put(norm, h);
            }
            // Исключаем служебные колонки (по нормализованным именам)
            Set<String> excluded = Set.of(
                    normalizeHeader(FILE_COL),
                    normalizeHeader(OBJECT_TYPE_COL),
                    normalizeHeader(CLASS_COL),
                    normalizeHeader(DOC_TYPE),
                    normalizeHeader(DOC_SUBTYPE),
                    normalizeHeader(ORDER_TYPE),
                    normalizeHeader(ADDITIONAL_CLASSES),
                    normalizeHeader(SINGLE_FILE),
                    normalizeHeader(DATE_CREATED),
                    normalizeHeader(DATE_MODIFIED),
                    // частые служебные поля M-Files, чтобы не создавать PropertyDef:
                    normalizeHeader("Permissions"),
                    normalizeHeader("Version"),
                    normalizeHeader("ID"),
                    normalizeHeader("Name"),
                    normalizeHeader("Name or title"),
                    normalizeHeader("Last modified by"),
                    normalizeHeader("Created by"),
                    normalizeHeader("Class [id<:ver>]")
            );
            for (CSVRecord row : parser) {
                stats.total++;
                try {
                    importRow(row, headers, headerAlias, excluded, filesDir, vault, stats);
                    stats.success++;
                } catch (Exception e) {
                    stats.failed++;
                    String msg = String.format("Ошибка импорта строки #%d: %s", row.getRecordNumber(), e.getMessage());
                    log.error("❌ {}", msg, e);
                    stats.errors.add(msg);
                }
            }
        } catch (Exception e) {
            log.error("❌ Ошибка чтения/парсинга CSV: {}", e.getMessage(), e);
            stats.errors.add("Ошибка чтения/парсинга CSV: " + e.getMessage());
        }
        log.info("✅ Импорт завершён: всего {}, успешно {}, ошибок {}, отсутствует файлов {}",
                stats.total, stats.success, stats.failed, stats.missingFiles);
        return new ImportSummary(stats.total, stats.success, stats.failed, stats.missingFiles, stats.errors);
    }

    private VaultEntity resolveVault(Long vaultId) {
        if (vaultId == null) {
            throw new IllegalArgumentException("Vault ID обязателен для импорта");
        }

        // Если таблица пустая — создаём дефолтный Vault
        if (vaultService.count() == 0) {
            log.warn("⚠️ В таблице Vault нет записей — создаём дефолтный Vault (id=1, code='MAIN')");
            VaultEntity defaultVault = new VaultEntity();
            defaultVault.setId(1L);
            defaultVault.setCode("MAIN");
            defaultVault.setName("Main Vault");
            defaultVault.setDescription("Automatically created default vault");
            defaultVault.setActive(true);

            // Создаём файловое хранилище, если его нет
            FileStorageRequest fsRequest = new FileStorageRequest();
            fsRequest.setKind(StorageKindEnum.FS);
            fsRequest.setName("Default File Storage");
            fsRequest.setDescription("Auto-created storage for default vault");
            fsRequest.setBasePath("/data/anubis/files");
            fsRequest.setDefaultStorage(true);
            fsRequest.setActive(true);

            FileStorageEntity storage = fileStorageAdminService.create(fsRequest);
            defaultVault.setDefaultStorage(storage);

            vaultService.save(defaultVault);
            log.info("✅ Создан дефолтный Vault '{}' с файловым хранилищем '{}'", defaultVault.getName(), storage.getName());
        }

        VaultEntity vault = vaultService.getVaultById(vaultId);
        if (vault == null) {
            throw new IllegalArgumentException("Vault с ID " + vaultId + " не найден");
        }
        return vault;
    }

    private VaultEntity resolveDefaultVault() {
        // Предпочтительно — по коду; при необходимости замените на ваш способ поиска
        VaultEntity vault = vaultService.getVaultByCode(defaultVaultCode);
        if (vault == null) {
            throw new IllegalStateException("Не найден Vault по коду: " + defaultVaultCode);
        }
        return vault;
    }

    private Path findCsv(Path dir) throws IOException {
        return Files.list(dir)
                .filter(p -> p.toString().toLowerCase().endsWith(".csv"))
                .findFirst()
                .orElseThrow(() -> new FileNotFoundException("CSV не найден в каталоге: " + dir));
    }

    /**
     * может вернуть null — если нет папки "(Files)"
     */
    private Path findFilesDir(Path dir) {
        try {
            return Files.list(dir)
                    .filter(p -> Files.isDirectory(p) && p.getFileName().toString().contains("(Files)"))
                    .findFirst()
                    .orElseThrow(() -> new FileNotFoundException("Папка '(Files)' не найдена в " + dir));
        } catch (Exception e) {
            log.warn("⚠️ Каталог '(Files)' отсутствует — файлы будут пропущены.");
            return null;
        }
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void importRow(
            CSVRecord row,
            Map<String, Integer> headers,
            Map<String, String> headerAlias,
            Set<String> excludedNormalized,
            Path filesDir,
            VaultEntity defaultVault,
            ImportStats stats
    ) throws Exception {

        String objectTypeName = safe(row, headerAlias, OBJECT_TYPE_COL, "Document");
        String className = safe(row, headerAlias, CLASS_COL, "Default");

        // ObjectType с привязкой к Vault
        ObjectType objectType = upsertObjectTypeWithVault(objectTypeName, defaultVault);
        ObjectClass objectClass = classService.upsertByName(objectType, className);

        // Гарантируем PropertyDef для SINGLE_FILE
        propertyDefService.findOrCreateDynamic(objectClass, SINGLE_FILE, PropertyDataType.BOOLEAN, false);

        // Имя объекта
        String objectName = Optional.ofNullable(value(row, headerAlias, "Name"))
                .orElse(Optional.ofNullable(value(row, headerAlias, "Name or title")).orElse(className));

        Optional<ObjectEntity> existing = objectService.findByTypeClassAndName(
                objectClass.getObjectType().getId(),
                objectClass.getId(),
                objectName
        );

        ObjectEntity obj;
        if (existing.isPresent()) {
            obj = existing.get();
            log.info("⚠️ Объект '{}' уже существует — обновление свойств", objectName);
        } else {
            ObjectDto dto = new ObjectDto();
            dto.setClassId(objectClass.getId());
            dto.setTypeId(objectClass.getObjectType().getId());
            dto.setName(objectName);
            obj = objectService.create(dto);
        }

        // --- фиксированные справочники и даты
        upsertValuelistAndSet(obj, objectClass, DOC_TYPE, value(row, headerAlias, DOC_TYPE), false);
        upsertValuelistAndSet(obj, objectClass, DOC_SUBTYPE, value(row, headerAlias, DOC_SUBTYPE), false);
        upsertValuelistAndSet(obj, objectClass, ORDER_TYPE, value(row, headerAlias, ORDER_TYPE), false);
        upsertValuelistAndSet(obj, objectClass, ADDITIONAL_CLASSES, value(row, headerAlias, ADDITIONAL_CLASSES), true);
        setBooleanIfPresent(obj, SINGLE_FILE, value(row, headerAlias, SINGLE_FILE));
        setDateIfPresent(obj, DATE_CREATED, value(row, headerAlias, DATE_CREATED));
        setDateIfPresent(obj, DATE_MODIFIED, value(row, headerAlias, DATE_MODIFIED));

        // --- динамические свойства
        for (String colRaw : headers.keySet()) {
            if (excludedNormalized.contains(normalizeHeader(colRaw))) continue;
            // Игнорируем служебные колонки вида "[id<:ver>]" — по ним не создаём PropertyDef
            if (isIdVerColumn(colRaw)) {
                continue;
            }

            String col = colRaw;
            String raw = value(row, headerAlias, col);
            if (raw == null || raw.isBlank()) continue;

            // Проверяем, есть ли у этой колонки пара [id<:ver>]
            boolean hasPair = hasIdVerPair(headers, col);
            boolean isMulti = looksLikeMulti(raw);
            PropertyDataType type = hasPair ? PropertyDataType.VALUELIST : guessType(col, raw, isMulti);

            // Очистка системного префикса # и фильтрация системных колонок
            String cleanCol = col != null && col.startsWith("#") ? col.substring(1).trim() : col;
            Set<String> SYSTEM_FIELDS = Set.of(
                "Accessed by Me", "Modified by", "Created by", "Class", "Workflow",
                "Single file", "Object ID", "Deleted"
            );
            if (SYSTEM_FIELDS.contains(cleanCol)) {
                log.debug("⚙️ Пропущено системное поле '{}'", cleanCol);
                continue;
            }

            // Ищем PropertyDef, уже принадлежащий классу, или создаём на лету
            Optional<PropertyDef> existingDef =
                    propertyDefRepository.findByClassIdAndNameIgnoreCase(objectClass.getId(), cleanCol);
            PropertyDef def;
            if (existingDef.isEmpty()) {
                PropertyDataType inferredType = hasPair ? PropertyDataType.VALUELIST : guessType(col, raw, isMulti);
                def = propertyDefService.findOrCreateDynamic(objectClass, cleanCol, inferredType, isMulti);
                log.info("🆕 Автоматически создано свойство '{}' для класса '{}'", cleanCol, objectClass.getName());
            } else {
                def = existingDef.get();
            }

            // --- если есть пара колонок (valuelist / multivaluelist)
            if (hasPair) {
                // Не используем числовые ID из CSV: создаём/находим элементы по ИМЕНИ (значению основной колонки)
                if (isMulti) {
                    List<Long> ids = ensureValueListItems(def.getName(), splitMulti(raw));
                    objectService.setValueMulti(obj, def, ids);
                } else {
                    Long id = ensureValueListItem(def.getName(), raw.trim());
                    objectService.setValue(obj, def, id);
                }
                continue;
            }

            // --- обычные свойства
            switch (def.getDataType()) {
                case BOOLEAN -> objectService.setValue(obj, def, parseBoolean(raw));
                case DATE -> {
                    LocalDateTime dt = tryParseDate(raw);
                    if (dt != null) objectService.setValue(obj, def, dt.toLocalDate().atStartOfDay());
                }
                default -> objectService.setValue(obj, def, raw);
            }
        }

        // --- обработка файла
        String filePath = value(row, headerAlias, FILE_COL);
        if (filePath != null && !filePath.isBlank()) {
            if (filesDir == null) {
                stats.missingFiles++;
                log.warn("🚫 Папка '(Files)' отсутствует — пропущен файл: {}", filePath);
                return;
            }
            Path normalized = normalizeFilePath(filesDir, filePath);
            if (Files.exists(normalized)) {
                try (InputStream is = Files.newInputStream(normalized)) {
                    MockMultipartFile multipart = new MockMultipartFile(
                            normalized.getFileName().toString(),
                            normalized.getFileName().toString(),
                            Files.probeContentType(normalized),
                            is
                    );
                    fileService.saveFile(obj.getId(), multipart);
                }
            } else {
                stats.missingFiles++;
                log.warn("🚫 Файл отсутствует: {}", normalized);
            }
        }
    }


    /**
     * upsert ObjectType с привязкой к Vault.
     * - Ищет ObjectType по имени.
     * - Если не найден, создаёт, устанавливает имя и vault, сохраняет и логгирует создание.
     * - Если найден, но vault не установлен или отличается, устанавливает vault и сохраняет.
     * - Возвращает ObjectType.
     */
    private ObjectType upsertObjectTypeWithVault(String name, VaultEntity vault) {
        ObjectType type = objectTypeService.findByName(name);
        if (type == null) {
            type = new ObjectType();
            type.setName(name);
            type.setVault(vault);
            type = objectTypeService.save(type);
            log.info("🆕 Создан ObjectType '{}' и привязан к Vault '{}'", name, vault.getCode());
        } else if (type.getVault() == null || !Objects.equals(type.getVault().getId(), vault.getId())) {
            type.setVault(vault);
            type = objectTypeService.save(type);
            log.info("🔗 ObjectType '{}' привязан к Vault '{}'", name, vault.getCode());
        }
        return type;
    }

    /**
     * Специальный апсерт для валюлистов + установка значения(й)
     */
    private void upsertValuelistAndSet(ObjectEntity obj, ObjectClass objectClass, String propName, String raw, boolean isMulti) {
        if (raw == null || raw.isBlank()) return;

        // создаём PropertyDef
        PropertyDef def = propertyDefService.findOrCreateDynamic(objectClass, propName, PropertyDataType.VALUELIST, isMulti);
        if (def == null) {
            log.warn("⚠️ Пропущена установка значения для '{}': PropertyDef не найден у класса '{}'",
                    propName, objectClass.getName());
            return;
        }

        // создаём / находим ValueList
        ValueList vl = valueListService.upsertByName(propName);

        // связываем PropertyDef с ValueList, если ещё не связано
        if (def.getValueList() == null || !Objects.equals(def.getValueList().getId(), vl.getId())) {
            def.setValueList(vl);
            propertyDefRepository.save(def); // 🧩 сохраняем связь
            log.info("🔗 Связали PropertyDef '{}' с ValueList '{}'", def.getName(), vl.getName());
        }

        // далее установка значений
        setValuelist(obj, def, raw, isMulti);
    }


    /**
     * Унифицированная установка VALUELIST (single/multi) для уже созданного PropertyDef
     */
    private void setValuelist(ObjectEntity obj, PropertyDef def, String raw, boolean isMulti) {
        if (raw == null || raw.isBlank()) return;

        ValueList vl = valueListService.upsertByName(def.getName());
        if (def.getValueList() == null || !Objects.equals(def.getValueList().getId(), vl.getId())) {
            def.setValueList(vl);
            propertyDefRepository.save(def);
            log.info("🔗 Связали PropertyDef '{}' с ValueList '{}'", def.getName(), vl.getName());
        }

        if (isMulti) {
            List<Long> ids = ensureValueListItems(def.getName(), splitMulti(raw));
            objectService.setValueMulti(obj, def, ids);
        } else {
            Long id = ensureValueListItem(def.getName(), raw.trim());
            objectService.setValue(obj, def, id);
        }
    }


    private List<String> splitMulti(String raw) {
        return Arrays.stream(raw.split(MULTI_SPLIT_REGEX))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }

    private boolean looksLikeMulti(String raw) {
        return raw != null && raw.matches(".*[;,].*");
    }

    private Long ensureValueListItem(String valueListName, String itemName) {
        ValueList vl = valueListService.upsertByName(valueListName);
        ValueListItem item = valueListService.upsertItem(vl, itemName);
        return item.getId();
    }

    private List<Long> ensureValueListItems(String valueListName, List<String> items) {
        ValueList vl = valueListService.upsertByName(valueListName);
        List<Long> ids = new ArrayList<>(items.size());
        for (String name : items) {
            ValueListItem item = valueListService.upsertItem(vl, name);
            ids.add(item.getId());
        }
        return ids;
    }

    private void setBooleanIfPresent(ObjectEntity obj, String prop, String raw) {
        if (raw == null || raw.isBlank()) return;
        objectService.setValue(obj, prop, parseBoolean(raw));
    }

    private void setDateIfPresent(ObjectEntity obj, String prop, String raw) {
        if (raw == null || raw.isBlank()) return;
        LocalDateTime dt = tryParseDate(raw);
        if (dt != null) objectService.setValue(obj, prop, dt);
    }

    // ===== ALIAS- and normalization-aware helpers =====
    private String normalizeHeader(String s) {
        if (s == null) return "";
        return s.replace("\uFEFF", "").trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String value(CSVRecord row, Map<String, String> headerAlias, String col) {
        String key = headerAlias.getOrDefault(normalizeHeader(col), col);
        try {
            return Optional.ofNullable(row.get(key)).map(String::trim).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private String safe(CSVRecord row, Map<String, String> headerAlias, String col, String def) {
        String v = value(row, headerAlias, col);
        return (v == null || v.isBlank()) ? def : v;
    }

    // Simple overloads (no alias) -- kept for backward compatibility, not used in import flow
    private String safe(CSVRecord row, String col, String def) {
        try {
            String v = row.get(col);
            return (v == null || v.isBlank()) ? def : v.trim();
        } catch (Exception e) {
            return def;
        }
    }

    private String value(CSVRecord row, String col) {
        try {
            return Optional.ofNullable(row.get(col)).map(String::trim).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private PropertyDataType guessType(String name, String value, boolean isMulti) {
        String n = name.toLowerCase(Locale.ROOT);
        if (n.contains("date") || n.contains("created") || n.contains("modified")) return PropertyDataType.DATE;
        if (Set.of("yes", "no", "true", "false", "1", "0").contains(value.toLowerCase(Locale.ROOT)))
            return PropertyDataType.BOOLEAN;
        // По умолчанию считаем текстом; VALUELIST определяется только наличием пары [id<:ver>]
        return PropertyDataType.TEXT;
    }

    private boolean parseBoolean(String v) {
        String s = v.trim().toLowerCase(Locale.ROOT);
        return s.equals("yes") || s.equals("true") || s.equals("1");
    }

    private LocalDateTime tryParseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        List<String> fmts = List.of("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd",
                "M/d/yyyy H:mm", "M/d/yyyy");
        for (String f : fmts) {
            try {
                return LocalDateTime.parse(raw.trim(), DateTimeFormatter.ofPattern(f));
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private Path normalizeFilePath(Path baseDir, String csvPath) {
        // ".\newbackup (Files)\Name (ID 123).pdf" -> "Name.pdf"
        String rel = csvPath.replace("\\", File.separator).replace(".\\", "");
        String baseName = rel.substring(rel.lastIndexOf(File.separator) + 1);
        int idIdx = baseName.lastIndexOf(" (ID ");
        if (idIdx != -1) {
            int dot = baseName.lastIndexOf('.');
            if (dot > idIdx) {
                baseName = baseName.substring(0, idIdx) + baseName.substring(dot);
            }
        }
        return baseDir.resolve(baseName);
    }

    /**
     * статистика процесса
     */
    private static class ImportStats {
        long total = 0;
        long success = 0;
        long failed = 0;
        long missingFiles = 0;
        final List<String> errors = new ArrayList<>();
    }

    /**
     * JSON-результат
     */
    public record ImportSummary(
            long total,
            long success,
            long failed,
            long missingFiles,
            List<String> errors
    ) {
    }
}
