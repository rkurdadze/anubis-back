package ge.comcom.anubis.service.core;

import ge.comcom.anubis.dto.ObjectVersionDto;
import ge.comcom.anubis.entity.core.ObjectEntity;
import ge.comcom.anubis.entity.core.ObjectVersionEntity;
import ge.comcom.anubis.entity.core.PropertyValue;
import ge.comcom.anubis.entity.core.PropertyValueMulti;
import ge.comcom.anubis.entity.security.User;
import ge.comcom.anubis.entity.meta.ClassProperty;
import ge.comcom.anubis.entity.meta.PropertyDef;
import ge.comcom.anubis.enums.VersionChangeType;
import ge.comcom.anubis.mapper.ObjectVersionMapper;
import ge.comcom.anubis.repository.core.ObjectRepository;
import ge.comcom.anubis.repository.core.ObjectVersionRepository;
import ge.comcom.anubis.repository.meta.ClassPropertyRepository;
import ge.comcom.anubis.repository.meta.PropertyValueRepository;
import ge.comcom.anubis.repository.core.PropertyValueMultiRepository;
import ge.comcom.anubis.repository.security.UserRepository;
import ge.comcom.anubis.util.UserContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
    private final PropertyValueMultiRepository propertyValueMultiRepository;
    private final ObjectVersionMapper versionMapper;
    private final UserRepository userRepository;

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
        Long actorId = saved.getCreatedBy() != null ? saved.getCreatedBy().getId() : null;
        String actorName = saved.getCreatedBy() != null ? saved.getCreatedBy().getUsername() : "unknown";
        auditService.logAction(saved, VersionChangeType.VERSION_SAVED, actorId,
                "Version saved/updated by " + actorName);
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
        List<ClassProperty> bindings = classPropertyRepository.findAllByObjectClass_IdOrderByDisplayOrderAsc(classId);
        var values = resolvePropertyValues(version);

        if (version.getId() == null && values.isEmpty()) {
            log.debug("No metadata provided for new version validation, skipping checks for version of object {}", obj.getId());
            return;
        }

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
     * Supports TEXT, NUMBER, DATE, BOOLEAN, LOOKUP, VALUELIST (with optional multiselect).
     */
    private void validateValueType(PropertyDef def, String valueText) {
        if (valueText == null || valueText.isBlank()) return;

        if (def.getDataType() == null) {
            throw new IllegalStateException("Data type not defined for property: " + def.getName());
        }

        try {
            switch (def.getDataType()) {
                case TEXT -> { /* always valid */ }
                case NUMBER -> new BigDecimal(valueText);
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
                case LOOKUP -> {
                    Long.parseLong(valueText);
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

    private List<PropertyValue> resolvePropertyValues(ObjectVersionEntity version) {
        if (version.getPropertyValues() != null && !version.getPropertyValues().isEmpty()) {
            return version.getPropertyValues();
        }
        if (version.getId() == null) {
            return List.of();
        }
        return propertyValueRepository.findAllByObjectVersionId(version.getId());
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
        return createNewVersion(objectId, comment, Instant.now(), null);
    }

    public ObjectVersionEntity createNewVersion(Long objectId, String comment, Instant createdAt, Instant modifiedAt) {
        ObjectEntity object = objectRepository.findById(objectId)
                .orElseThrow(() -> new EntityNotFoundException("Object not found: " + objectId));

        Integer lastVersion = versionRepository.findLastVersionNumber(objectId);
        ObjectVersionEntity previousVersion = null;
        if (lastVersion != null) {
            previousVersion = versionRepository.findTopByObject_IdOrderByVersionNumberDesc(objectId)
                    .orElse(null);
        }

        int newVersion = (lastVersion == null ? 1 : lastVersion + 1);

        ObjectVersionEntity entity = ObjectVersionEntity.builder()
                .object(object)
                .versionNumber(newVersion)
                .comment(comment != null ? comment : "Auto-created version")
                .build();

        Instant effectiveCreatedAt = createdAt != null ? createdAt : Instant.now();
        entity.setCreatedAt(effectiveCreatedAt);
        entity.setModifiedAt(modifiedAt != null ? modifiedAt : effectiveCreatedAt);

        var currentUser = UserContext.getCurrentUser();
        Optional<User> author = resolveOrProvisionUser(currentUser);
        author.ifPresent(entity::setCreatedBy);

        ObjectVersionEntity saved = versionRepository.save(entity);
        copyPropertyValues(previousVersion, saved);
        Long actorId = author.map(User::getId).orElse(null);
        auditService.logAction(saved, VersionChangeType.VERSION_CREATED, actorId,
                "Created new version " + newVersion);

        log.info("Auto-created version {} for object {} (createdAt={})", newVersion, objectId, saved.getCreatedAt());
        return saved;
    }

    /**
     * Returns an existing version with the specified comment or creates a new one while updating timestamps.
     * Used during imports to avoid creating redundant versions for the same snapshot.
     */
    public VersionAcquisition acquireVersionForComment(
            Long objectId,
            String comment,
            Instant createdAt,
            Instant modifiedAt
    ) {
        Optional<ObjectVersionEntity> existing = versionRepository
                .findTopByObject_IdAndCommentIgnoreCaseOrderByVersionNumberDesc(objectId, comment);

        if (existing.isPresent()) {
            ObjectVersionEntity version = existing.get();
            boolean changed = false;

            if (createdAt != null && !Objects.equals(version.getCreatedAt(), createdAt)) {
                version.setCreatedAt(createdAt);
                changed = true;
            }

            Instant effectiveModified = modifiedAt != null
                    ? modifiedAt
                    : (createdAt != null ? createdAt : version.getModifiedAt());
            if (effectiveModified != null && !Objects.equals(version.getModifiedAt(), effectiveModified)) {
                version.setModifiedAt(effectiveModified);
                changed = true;
            }

            if (changed) {
                version = versionRepository.save(version);
            }

            return new VersionAcquisition(version, false);
        }

        Integer lastVersionNumber = versionRepository.findLastVersionNumber(objectId);
        if (lastVersionNumber != null && lastVersionNumber == 1) {
            ObjectVersionEntity singleVersion = versionRepository.findTopByObject_IdOrderByVersionNumberDesc(objectId)
                    .orElseThrow(() -> new EntityNotFoundException("Object has no versions: " + objectId));

            boolean changed = false;

            if (comment != null && !comment.equalsIgnoreCase(Optional.ofNullable(singleVersion.getComment()).orElse(""))) {
                singleVersion.setComment(comment);
                changed = true;
            }

            if (createdAt != null && !Objects.equals(singleVersion.getCreatedAt(), createdAt)) {
                singleVersion.setCreatedAt(createdAt);
                changed = true;
            }

            Instant effectiveModified = modifiedAt != null
                    ? modifiedAt
                    : (createdAt != null ? createdAt : singleVersion.getModifiedAt());
            if (effectiveModified != null && !Objects.equals(singleVersion.getModifiedAt(), effectiveModified)) {
                singleVersion.setModifiedAt(effectiveModified);
                changed = true;
            }

            if (changed) {
                singleVersion = versionRepository.save(singleVersion);
            }

            return new VersionAcquisition(singleVersion, false);
        }

        ObjectVersionEntity created = createNewVersion(objectId, comment, createdAt, modifiedAt);
        return new VersionAcquisition(created, true);
    }

    private Optional<User> resolveOrProvisionUser(User contextUser) {
        if (contextUser == null) {
            return Optional.empty();
        }

        if (contextUser.getId() != null) {
            Optional<User> byId = userRepository.findById(contextUser.getId());
            if (byId.isPresent()) {
                return byId;
            }
        }

        String username = contextUser.getUsername();
        if (username != null && !username.isBlank()) {
            Optional<User> byUsername = userRepository.findByUsernameIgnoreCase(username);
            if (byUsername.isPresent()) {
                return byUsername;
            }

            User provisioned = User.builder()
                    .username(username)
                    .fullName(contextUser.getFullName())
                    .build();
            User saved = userRepository.save(provisioned);
            log.info("Provisioned placeholder user '{}' with id {} for version tracking", saved.getUsername(), saved.getId());
            return Optional.of(saved);
        }

        log.warn("Unable to resolve current user for version tracking: missing identifier and username");
        return Optional.empty();
    }

    private void copyPropertyValues(ObjectVersionEntity source, ObjectVersionEntity target) {
        if (source == null || target == null) {
            return;
        }

        List<PropertyValue> sourceValues = propertyValueRepository.findAllByObjectVersionId(source.getId());
        if (sourceValues.isEmpty()) {
            return;
        }

        if (target.getPropertyValues() == null) {
            target.setPropertyValues(new ArrayList<>());
        }

        for (PropertyValue value : sourceValues) {
            PropertyValue clone = PropertyValue.builder()
                    .objectVersion(target)
                    .propertyDef(value.getPropertyDef())
                    .valueText(value.getValueText())
                    .valueNumber(value.getValueNumber())
                    .valueDate(value.getValueDate())
                    .valueBoolean(value.getValueBoolean())
                    .refObject(value.getRefObject())
                    .valueListItem(value.getValueListItem())
                    .build();

            PropertyValue persisted = propertyValueRepository.save(clone);
            target.getPropertyValues().add(persisted);

            List<PropertyValueMulti> multiValues = propertyValueMultiRepository.findAllByPropertyValueId(value.getId());
            for (PropertyValueMulti multi : multiValues) {
                PropertyValueMulti cloneMulti = new PropertyValueMulti();
                cloneMulti.setPropertyValue(persisted);
                cloneMulti.setValueListItem(multi.getValueListItem());
                propertyValueMultiRepository.save(cloneMulti);
            }
        }
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
        return versionRepository.findTopByObject_IdOrderByVersionNumberDesc(objectId)
                .orElseGet(() -> createNewVersion(objectId, "Auto-created version"));
    }

    public record VersionAcquisition(ObjectVersionEntity version, boolean createdNew) {
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
