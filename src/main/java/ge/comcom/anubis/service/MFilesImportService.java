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
import ge.comcom.anubis.service.core.ObjectVersionService;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private final ObjectVersionService objectVersionService;
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
    private static final String DATE_CREATED = "Date Created";
    private static final String DATE_MODIFIED = "Date Modified";
    private static final String IMPORT_VERSION_COMMENT = "Imported from M-Files backup";

    /**
     * Разделители для мультизначных колонок
     */
    private static final String MULTI_SPLIT_REGEX = "[;,]";

    private static final Pattern DATE_VALUE_PATTERN = Pattern.compile(
            "^\\s*\\d{1,4}([./-])\\d{1,2}\\1\\d{2,4}(\\s+\\d{1,2}:\\d{2}(?::\\d{2})?(\\s?[APap][mM])?)?\\s*$"
    );

    private static final Pattern ISO_DATE_PATTERN = Pattern.compile(
            "^\\d{4}-\\d{2}-\\d{2}(?:[T\\s]\\d{2}:\\d{2}(?::\\d{2})?)?(?:Z)?$"
    );

    private static final Pattern TWO_DIGIT_YEAR_PATTERN = Pattern.compile(
            "^(\\s*\\d{1,2}[./-]\\d{1,2}[./-])(\\d{2})(.*)$"
    );

    private static final ZoneId IMPORT_ZONE = ZoneId.systemDefault();

    private static final List<DateFormatSpec> DATE_FORMATS = List.of(
            format("M/d/uuuu HH:mm", false),
            format("M/d/uuuu H:mm", false),
            format("M/d/uuuu hh:mm a", false),
            format("M/d/uuuu h:mm a", false),
            format("M/d/uuuu HH:mm:ss", false),
            format("M/d/uuuu hh:mm:ss a", false),
            format("M/d/uuuu", true),
            format("d.M.uuuu HH:mm", false),
            format("d.M.uuuu H:mm", false),
            format("d.M.uuuu HH:mm:ss", false),
            format("d.M.uuuu", true),
            format("uuuu-MM-dd HH:mm:ss", false),
            format("uuuu-MM-dd'T'HH:mm:ss", false),
            format("uuuu-MM-dd", true)
    );

    private static final Set<String> BOOLEAN_LITERALS = Set.of("yes", "no", "true", "false", "1", "0");

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

        Instant versionCreatedAt = parseDateToInstant(value(row, headerAlias, DATE_CREATED));
        Instant versionModifiedAt = parseDateToInstant(value(row, headerAlias, DATE_MODIFIED));

        ObjectVersionService.VersionAcquisition versionAcquisition = objectVersionService.acquireVersionForComment(
                obj.getId(),
                IMPORT_VERSION_COMMENT,
                versionCreatedAt,
                versionModifiedAt
        );
        ObjectVersionEntity importVersion = versionAcquisition.version();
        boolean createdVersion = versionAcquisition.createdNew();

        try {
            // --- динамические свойства
            for (String colRaw : headers.keySet()) {
                if (excludedNormalized.contains(normalizeHeader(colRaw))) continue;
                if (isIdVerColumn(colRaw)) {
                    continue;
                }

                String raw = value(row, headerAlias, colRaw);
                if (raw == null || raw.isBlank()) continue;

                boolean hasPair = hasIdVerPair(headers, colRaw);
                boolean isMulti = looksLikeMulti(raw);
                String cleanCol = colRaw != null && colRaw.startsWith("#") ? colRaw.substring(1).trim() : colRaw;

                Set<String> SYSTEM_FIELDS = Set.of(
                        "Accessed by Me", "Modified by", "Created by", "Class", "Workflow",
                        "Object ID", "Deleted"
                );
                if (SYSTEM_FIELDS.contains(cleanCol)) {
                    log.debug("⚙️ Пропущено системное поле '{}'", cleanCol);
                    continue;
                }

                PropertyDataType inferredType = hasPair ? PropertyDataType.VALUELIST : guessType(cleanCol, raw, isMulti);

                Optional<PropertyDef> existingDef =
                        propertyDefRepository.findByClassIdAndNameIgnoreCase(objectClass.getId(), cleanCol);
                PropertyDef def = existingDef.orElseGet(() -> {
                    PropertyDataType typeToUse = hasPair ? PropertyDataType.VALUELIST : inferredType;
                    PropertyDef created = propertyDefService.findOrCreateDynamic(objectClass, cleanCol, typeToUse, isMulti);
                    log.info("🆕 Автоматически создано свойство '{}' для класса '{}'", cleanCol, objectClass.getName());
                    return created;
                });

                if (hasPair) {
                    def = ensureValueListProperty(def, isMulti);
                    if (isMulti) {
                        List<Long> ids = ensureValueListItems(def, splitMulti(raw));
                        objectService.setValueMulti(obj, def, ids);
                    } else {
                        Long id = ensureValueListItem(def, raw.trim());
                        objectService.setValue(obj, def, id);
                    }
                    continue;
                }

                switch (def.getDataType()) {
                    case BOOLEAN -> objectService.setValue(obj, def, parseBoolean(raw));
                    case DATE -> {
                        LocalDateTime dt = tryParseDate(raw);
                        if (dt != null) {
                            objectService.setValue(obj, def, dt.toLocalDate());
                        }
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
                        FileService.SaveOptions options = FileService.SaveOptions.builder()
                                .skipIndexing(true)
                                .targetVersionId(importVersion.getId())
                                .versionComment(importVersion.getComment())
                                .build();
                        fileService.saveFile(obj.getId(), multipart, options);
                    }
                } else {
                    stats.missingFiles++;
                    log.warn("🚫 Файл отсутствует: {}", normalized);
                }
            }
        } catch (Exception processingError) {
            if (createdVersion) {
                try {
                    objectVersionService.deleteVersion(importVersion.getId());
                } catch (Exception cleanupError) {
                    log.error("Не удалось удалить версию {} после ошибки импорта: {}", importVersion.getId(), cleanupError.getMessage());
                }
            }
            throw processingError;
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

    private PropertyDef ensureValueListProperty(PropertyDef def, Boolean shouldBeMultiselect) {
        boolean changed = false;

        if (def.getDataType() != PropertyDataType.VALUELIST) {
            def.setDataType(PropertyDataType.VALUELIST);
            changed = true;
        }

        boolean currentMulti = Boolean.TRUE.equals(def.getIsMultiselect());
        if (shouldBeMultiselect != null && shouldBeMultiselect != currentMulti) {
            def.setIsMultiselect(shouldBeMultiselect);
            changed = true;
        }

        ValueList ensuredList = valueListService.upsertByName(def.getName());
        if (def.getValueList() == null || !Objects.equals(def.getValueList().getId(), ensuredList.getId())) {
            def.setValueList(ensuredList);
            changed = true;
        }

        if (changed) {
            def = propertyDefRepository.save(def);
        }

        return def;
    }

    private Long ensureValueListItem(PropertyDef def, String itemName) {
        PropertyDef synced = def.getValueList() == null ? ensureValueListProperty(def, null) : def;
        ValueList target = synced.getValueList();
        if (target == null || target.getId() == null) {
            throw new IllegalStateException("ValueList is not associated with property '" + synced.getName() + "'");
        }
        ValueListItem item = valueListService.upsertItem(target.getId(), itemName);
        return item.getId();
    }

    private List<Long> ensureValueListItems(PropertyDef def, List<String> items) {
        PropertyDef synced = ensureValueListProperty(def, true);
        ValueList target = synced.getValueList();
        if (target == null || target.getId() == null) {
            throw new IllegalStateException("ValueList is not associated with property '" + synced.getName() + "'");
        }
        List<Long> ids = new ArrayList<>(items.size());
        for (String name : items) {
            ValueListItem item = valueListService.upsertItem(target.getId(), name);
            ids.add(item.getId());
        }
        return ids;
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
        String normalizedName = name == null ? "" : name.toLowerCase(Locale.ROOT);
        String normalizedValue = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);

        if (!normalizedValue.isEmpty() && looksLikeDateValue(value)) {
            return PropertyDataType.DATE;
        }
        if (normalizedName.contains("date") || normalizedName.contains("created") || normalizedName.contains("modified")) {
            return PropertyDataType.DATE;
        }
        if (BOOLEAN_LITERALS.contains(normalizedValue)) {
            return PropertyDataType.BOOLEAN;
        }
        return PropertyDataType.TEXT;
    }

    private boolean parseBoolean(String v) {
        String s = v == null ? "" : v.trim().toLowerCase(Locale.ROOT);
        return s.equals("yes") || s.equals("true") || s.equals("1");
    }

    private LocalDateTime tryParseDate(String raw) {
        return parseFlexibleDateTime(raw);
    }

    private boolean looksLikeDateValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String trimmed = raw.trim();
        return DATE_VALUE_PATTERN.matcher(trimmed).matches() || ISO_DATE_PATTERN.matcher(trimmed).matches();
    }

    private Instant parseDateToInstant(String raw) {
        LocalDateTime dt = parseFlexibleDateTime(raw);
        return dt == null ? null : dt.atZone(IMPORT_ZONE).toInstant();
    }

    private LocalDateTime parseFlexibleDateTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = normalizeTwoDigitYear(raw.trim());
        if (normalized.endsWith("Z") && normalized.contains("T")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        normalized = normalized.replaceAll("\\s+", " ");

        for (DateFormatSpec spec : DATE_FORMATS) {
            try {
                if (spec.dateOnly()) {
                    LocalDate date = LocalDate.parse(normalized, spec.formatter());
                    return date.atStartOfDay();
                }
                return LocalDateTime.parse(normalized, spec.formatter());
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private String normalizeTwoDigitYear(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        Matcher matcher = TWO_DIGIT_YEAR_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            int year = Integer.parseInt(matcher.group(2));
            int normalizedYear = year >= 50 ? 1900 + year : 2000 + year;
            return matcher.group(1) + normalizedYear + matcher.group(3);
        }
        return trimmed;
    }

    private static DateFormatSpec format(String pattern, boolean dateOnly) {
        return new DateFormatSpec(
                DateTimeFormatter.ofPattern(pattern)
                        .withLocale(Locale.US)
                        .withResolverStyle(ResolverStyle.STRICT),
                dateOnly
        );
    }

    private record DateFormatSpec(DateTimeFormatter formatter, boolean dateOnly) {
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
