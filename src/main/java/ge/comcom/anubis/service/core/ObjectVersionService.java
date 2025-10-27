package ge.comcom.anubis.service.core;

import ge.comcom.anubis.dto.ObjectVersionDto;
import ge.comcom.anubis.entity.core.ObjectEntity;
import ge.comcom.anubis.entity.core.ObjectVersionEntity;
import ge.comcom.anubis.entity.meta.ClassProperty;
import ge.comcom.anubis.entity.meta.PropertyDef;
import ge.comcom.anubis.enums.VersionChangeType;
import ge.comcom.anubis.mapper.ObjectVersionMapper;
import ge.comcom.anubis.repository.core.ObjectRepository;
import ge.comcom.anubis.repository.core.ObjectVersionRepository;
import ge.comcom.anubis.repository.meta.ClassPropertyRepository;
import ge.comcom.anubis.repository.meta.PropertyDefRepository;
import ge.comcom.anubis.repository.meta.PropertyValueRepository;
import ge.comcom.anubis.util.UserContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service layer for managing object versions and validating metadata.
 * Each version represents a snapshot of an object’s metadata and properties.
 *
 * <p>Validation ensures:</p>
 * <ul>
 *     <li>Presence of required fields</li>
 *     <li>Multi-value property rules</li>
 *     <li>Regex pattern validation</li>
 *     <li>Strict type enforcement (like M-Files)</li>
 *     <li>ValueList/ValueListItem reference consistency</li>
 *     <li>Unique value constraint within class scope</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ObjectVersionService {

    private final ObjectVersionRepository versionRepository;
    private final ObjectRepository objectRepository;
    private final ObjectVersionAuditService auditService;
    private final ClassPropertyRepository classPropertyRepository;
    private final PropertyValueRepository propertyValueRepository;
    private final ObjectVersionMapper versionMapper;

    /**
     * Saves or updates an object version, including full metadata validation.
     *
     * @param entity ObjectVersionEntity to be saved
     * @return saved ObjectVersionEntity
     */
    public ObjectVersionEntity save(ObjectVersionEntity entity) {
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }

        validateMetadata(entity);

        log.debug("Saving version for objectId={} versionNumber={}",
                entity.getObject() != null ? entity.getObject().getId() : "null",
                entity.getVersionNumber());

        ObjectVersionEntity saved = versionRepository.save(entity);
        auditService.logAction(saved, VersionChangeType.VERSION_SAVED, null,
                "Version saved/updated by " + entity.getCreatedBy());
        return saved;
    }

    /**
     * Validates metadata (property values) according to class-property bindings.
     * Ensures all logical, semantic and type-level rules are satisfied.
     */
    private void validateMetadata(ObjectVersionEntity version) {
        ObjectEntity obj = version.getObject();
        if (obj == null || obj.getObjectClass() == null) {
            log.warn("Skipping metadata validation: object or class is null for version {}", version.getId());
            return;
        }

        Long classId = obj.getObjectClass().getId();
        List<ClassProperty> bindings = classPropertyRepository.findAllByObjectClassIdOrderByDisplayOrderAsc(classId);
        var values = propertyValueRepository.findAllByObjectVersionId(version.getId());

        for (ClassProperty cp : bindings) {
            PropertyDef def = cp.getPropertyDef();
            var propValues = values.stream()
                    .filter(v -> v.getPropertyDef().getId().equals(def.getId()))
                    .toList();

            // 1️⃣ Required property check
            if (Boolean.TRUE.equals(def.getIsRequired()) && propValues.isEmpty()) {
                throw new IllegalStateException("Missing required property: " + def.getName());
            }

            // 2️⃣ Multiselect constraint
            if (!Boolean.TRUE.equals(def.getIsMultiselect()) && propValues.size() > 1) {
                throw new IllegalStateException("Property '" + def.getName() + "' must have only one value");
            }

            // 3️⃣ Regex pattern validation
            if (def.getRegex() != null && !def.getRegex().isBlank()) {
                Pattern pattern = Pattern.compile(def.getRegex());
                for (var pv : propValues) {
                    if (pv.getValueText() != null &&
                            !pattern.matcher(pv.getValueText()).matches()) {
                        throw new IllegalStateException(
                                String.format("Property '%s' does not match pattern: %s", def.getName(), def.getRegex()));
                    }
                }
            }

            // 4️⃣ Strict data type validation
            for (var pv : propValues) {
                validateValueType(def, pv.getValueText());
            }

            // 5️⃣ Uniqueness constraint within class scope
            if (Boolean.TRUE.equals(def.getIsUnique())) {
                for (var pv : propValues) {
                    boolean duplicate = propertyValueRepository.existsDuplicateInClass(
                            classId, def.getId(), pv.getValueText(), version.getId());
                    if (duplicate) {
                        throw new IllegalStateException(
                                "Duplicate value for unique property: " + def.getName());
                    }
                }
            }
        }
    }

    /**
     * Strictly validates property value type according to its definition.
     * Supports TEXT, INTEGER, FLOAT, BOOLEAN, DATE, VALUELIST, MULTI_VALUELIST.
     */
    private void validateValueType(PropertyDef def, String valueText) {
        if (valueText == null || valueText.isBlank()) return;

        if (def.getDataType() == null) {
            throw new IllegalStateException("Data type not defined for property: " + def.getName());
        }

        try {
            switch (def.getDataType()) {
                case TEXT -> { /* always valid */ }
                case INTEGER -> Integer.parseInt(valueText);
                case FLOAT -> Double.parseDouble(valueText);
                case BOOLEAN -> {
                    if (!valueText.equalsIgnoreCase("true") &&
                            !valueText.equalsIgnoreCase("false")) {
                        throw new IllegalStateException(
                                String.format("Invalid boolean value '%s' for property '%s'", valueText, def.getName()));
                    }
                }
                case DATE -> {
                    try {
                        OffsetDateTime.parse(valueText);
                    } catch (Exception e) {
                        LocalDate.parse(valueText);
                    }
                }
                case VALUELIST -> {
                    if (def.getValueList() == null) {
                        throw new IllegalStateException("Property '" + def.getName() + "' has no associated ValueList");
                    }
                    boolean exists = def.getValueList().getItems().stream()
                            .anyMatch(item -> item.getIsActive() &&
                                    item.getValue().equalsIgnoreCase(valueText));

                    if (!exists) {
                        throw new IllegalStateException(String.format(
                                "Value '%s' is not a valid item in ValueList '%s' for property '%s'",
                                valueText, def.getValueList().getName(), def.getName()));
                    }
                }
                case MULTI_VALUELIST -> {
                    if (def.getValueList() == null) {
                        throw new IllegalStateException("Property '" + def.getName() + "' has no associated ValueList");
                    }
                    String[] parts = valueText.split(",");
                    for (String raw : parts) {
                        String trimmed = raw.trim();
                        if (trimmed.isEmpty()) continue;

                        boolean exists = def.getValueList().getItems().stream()
                                .anyMatch(item -> item.getIsActive() &&
                                        item.getValue().equalsIgnoreCase(trimmed));

                        if (!exists) {
                            throw new IllegalStateException(String.format(
                                    "Value '%s' is not valid in MultiValueList '%s' for property '%s'",
                                    trimmed, def.getValueList().getName(), def.getName()));
                        }
                    }
                }
                default -> throw new IllegalStateException("Unsupported data type: " + def.getDataType());
            }
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    String.format("Invalid numeric value '%s' for property '%s'", valueText, def.getName()));
        } catch (Exception e) {
            throw new IllegalStateException(
                    String.format("Invalid value '%s' for property '%s': %s",
                            valueText, def.getName(), e.getMessage()));
        }
    }

    /**
     * Retrieves a version entity by ID.
     */
    @Transactional(readOnly = true)
    public ObjectVersionEntity getById(Long id) {
        return versionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Version not found with ID: " + id));
    }

    /**
     * Retrieves all versions associated with a specific object.
     */
    @Transactional(readOnly = true)
    public List<ObjectVersionEntity> getByObjectId(Long objectId) {
        return versionRepository.findByObject_IdOrderByVersionNumberAsc(objectId);
    }

    /**
     * Automatically creates a new version for a given object.
     */
    public ObjectVersionEntity createNewVersion(Long objectId, String comment) {
        ObjectEntity object = objectRepository.findById(objectId)
                .orElseThrow(() -> new EntityNotFoundException("Object not found: " + objectId));

        Integer lastVersion = versionRepository.findLastVersionNumber(objectId);
        int newVersion = (lastVersion == null ? 1 : lastVersion + 1);

        ObjectVersionEntity entity = ObjectVersionEntity.builder()
                .object(object)
                .versionNumber(newVersion)
                .createdAt(Instant.now())
                .createdBy(UserContext.getCurrentUser()) // TODO: replace with authenticated user
                .comment(comment != null ? comment : "Auto-created version")
                .build();

        ObjectVersionEntity saved = versionRepository.save(entity);
        auditService.logAction(saved, VersionChangeType.VERSION_CREATED, null,
                "Created new version " + newVersion);

        log.info("Auto-created version {} for object {}", newVersion, objectId);
        return saved;
    }

    /**
     * Returns the latest version for a given object.
     */
    @Transactional(readOnly = true)
    public ObjectVersionEntity getLatestVersion(Long objectId) {
        return versionRepository.findTopByObject_IdOrderByVersionNumberDesc(objectId).orElse(null);
    }

    /**
     * Returns latest version or creates if none exists.
     */
    public ObjectVersionEntity getOrCreateLatestVersion(Long objectId) {
        ObjectVersionEntity latest = getLatestVersion(objectId);
        return (latest != null)
                ? latest
                : createNewVersion(objectId, "Auto-created initial version");
    }



    @Transactional
    public void deleteVersion(Long versionId) {
        var version = versionRepository.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Version not found: " + versionId));
        versionRepository.delete(version);
    }

    @Transactional(readOnly = true)
    public ObjectVersionDto getVersionDto(Long versionId) {
        ObjectVersionEntity entity = getById(versionId);
        return versionMapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public List<ObjectVersionDto> getVersionsByObject(Long objectId) {
        return versionRepository.findByObject_IdOrderByVersionNumberDesc(objectId)
                .stream()
                .map(versionMapper::toDto)
                .toList();
    }
}
