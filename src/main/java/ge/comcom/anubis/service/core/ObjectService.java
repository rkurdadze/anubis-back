package ge.comcom.anubis.service.core;

import ge.comcom.anubis.dto.ObjectDto;
import ge.comcom.anubis.entity.core.*;
import ge.comcom.anubis.entity.meta.PropertyDef;
import ge.comcom.anubis.entity.security.User;
import ge.comcom.anubis.enums.LinkDirection;
import ge.comcom.anubis.mapper.ObjectMapper;
import ge.comcom.anubis.repository.core.*;
import ge.comcom.anubis.repository.core.FileBinaryRepository;
import ge.comcom.anubis.repository.meta.PropertyDefRepository;
import ge.comcom.anubis.repository.meta.PropertyValueRepository;
import ge.comcom.anubis.repository.meta.ValueListItemRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import ge.comcom.anubis.enums.PropertyDataType;

/**
 * Service layer for managing logical repository objects.
 * Handles CRUD operations with soft-delete logic and audit safety.
 * <p>
 * <strong>Link management is delegated to {@link ObjectLinkService}</strong>.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ObjectService {

    private final ObjectRepository objectRepository;
    private final ObjectLinkService linkService;
    private final ObjectLinkRepository linkRepository; // Только для incoming links (если нет в linkService)
    private final ObjectTypeRepository objectTypeRepository;
    private final ObjectClassRepository objectClassRepository;
    private final ObjectMapper objectMapper;
    private final ObjectVersionService objectVersionService;
    private final ObjectVersionAuditService auditService;

    private final FileBinaryRepository fileBinaryRepository;


    private final PropertyDefRepository propertyDefRepository;
    private final PropertyValueRepository propertyValueRepository;
    private final PropertyValueMultiRepository propertyValueMultiRepository;
    private final ValueListItemRepository valueListItemRepository;

    /**
     * Creates a new object.
     *
     * @param dto payload describing the object to create
     * @return persisted entity
     */
    public ObjectEntity create(ObjectDto dto) {
        ObjectEntity entity = objectMapper.toEntity(dto);
        applyRelations(entity, dto);
        entity.setIsDeleted(false);
        log.info("Creating new object '{}'", entity.getName());

        ObjectEntity saved = objectRepository.save(entity);

        ObjectVersionEntity initialVersion = objectVersionService.createNewVersion(
                saved.getId(),
                "Initial version after object creation"
        );

        if (saved.getVersions() != null) {
            saved.getVersions().add(initialVersion);
        }

        return saved;
    }

    /**
     * Updates existing object (only editable fields).
     *
     * @param id  object ID
     * @param dto new state
     * @return updated entity
     */
    public ObjectEntity update(Long id, ObjectDto dto) {
        ObjectEntity existing = getById(id);
        ObjectStateSnapshot beforeUpdate = ObjectStateSnapshot.from(existing);

        objectMapper.updateEntityFromDto(dto, existing);
        applyRelations(existing, dto);

        log.info("Updating object ID {}", id);
        ObjectEntity saved = objectRepository.save(existing);

        ObjectVersionEntity newVersion = objectVersionService.createNewVersion(
                saved.getId(),
                "Auto-created version after object update"
        );

        if (saved.getVersions() != null) {
            saved.getVersions().add(newVersion);
        }

        logObjectFieldChanges(beforeUpdate, saved, newVersion);

        return saved;
    }

    /**
     * Performs soft delete (sets isDeleted=true).
     *
     * @param id   object ID
     * @param user user performing deletion (nullable)
     */
    public void softDelete(Long id, User user) {
        ObjectEntity entity = getById(id);
        entity.setIsDeleted(true);
        entity.setDeletedAt(Instant.now());
        entity.setDeletedBy(user);
        objectRepository.save(entity);
        log.warn("Soft-deleted object ID {} by {}", id, user != null ? user.getUsername() : "system");
    }

    /**
     * Permanently removes object from database.
     *
     * @param id object ID
     */
    public void hardDelete(Long id) {
        if (!objectRepository.existsById(id)) {
            throw new EntityNotFoundException("Object not found: " + id);
        }
        objectRepository.deleteById(id);
        log.warn("Hard-deleted object ID {}", id);
        // 🧹 Очистка осиротевших бинарных файлов
        if (fileBinaryRepository != null) {
            List<Long> orphanIds = fileBinaryRepository.findOrphans();
            if (!orphanIds.isEmpty()) {
                fileBinaryRepository.deleteAllById(orphanIds);
                log.info("🧹 Removed {} orphan file binaries after hardDelete({})", orphanIds.size(), id);
            }
        }
    }

    /**
     * Returns all active (non-deleted) objects.
     */
    @Transactional(readOnly = true)
    public List<ObjectEntity> getAllActive() {
        List<ObjectEntity> objects = objectRepository.findByIsDeletedFalse();
        objects.forEach(this::initializeForRead);
        return objects;
    }

    @Transactional(readOnly = true)
    public Page<ObjectEntity> getAllActive(Pageable pageable) {
        Page<ObjectEntity> page = objectRepository.findByIsDeletedFalse(pageable);
        page.forEach(this::initializeForRead);
        return page;
    }

    /**
     * Retrieves object by ID.
     */
    @Transactional(readOnly = true)
    public ObjectEntity getById(Long id) {
        ObjectEntity entity = objectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Object not found: " + id));
        initializeForRead(entity);
        return entity;
    }

    /**
     * Retrieves object with all outgoing and incoming links (using JOIN FETCH).
     */
    @Transactional(readOnly = true)
    public ObjectEntity getWithLinks(Long id) {
        ObjectEntity entity = objectRepository.findByIdWithLinks(id)
                .orElseThrow(() -> new EntityNotFoundException("Object not found: " + id));
        initializeForRead(entity);
        return entity;
    }

    // === Делегирование в ObjectLinkService ===

    /**
     * Creates a link (UNI or BI). BI creates reciprocal link.
     */
    public ObjectLinkEntity createLink(Long sourceId, Long targetId, String role, LinkDirection direction) {
        return linkService.createLink(sourceId, targetId, role, direction);
    }

    /**
     * Removes all links between two objects with given role (both directions).
     */
    public void removeLink(Long sourceId, Long targetId, String role) {
        linkService.removeLink(sourceId, targetId, role);
    }

    /**
     * Returns all outgoing links from the object.
     */
    @Transactional(readOnly = true)
    public List<ObjectLinkEntity> getOutgoingLinks(Long objectId) {
        return linkService.getLinks(objectId);
    }

    /**
     * Returns all incoming links to the object.
     */
    @Transactional(readOnly = true)
    public List<ObjectLinkEntity> getIncomingLinks(Long objectId) {
        return linkRepository.findByTarget_Id(objectId);
    }

    private void applyRelations(ObjectEntity entity, ObjectDto dto) {
        ObjectType type = resolveType(dto.getTypeId());
        entity.setObjectType(type);
        entity.setObjectClass(resolveClass(dto.getClassId()));
        ensureVaultConsistency(dto.getVaultId(), type);
    }

    private void logObjectFieldChanges(ObjectStateSnapshot before,
                                       ObjectEntity after,
                                       ObjectVersionEntity version) {
        if (before == null || after == null || version == null) {
            return;
        }

        Long actorId = version.getCreatedBy() != null ? version.getCreatedBy().getId() : null;

        if (!equalsSafe(before.name(), after.getName())) {
            auditService.logFieldChange(
                    version,
                    "name",
                    before.name(),
                    after.getName(),
                    actorId,
                    buildSummary("name", before.name(), after.getName())
            );
        }

        if (!equalsSafe(before.typeId(), extractId(after.getObjectType()))) {
            auditService.logFieldChange(
                    version,
                    "objectType",
                    before.typeDisplay(),
                    formatLookup(after.getObjectType()),
                    actorId,
                    buildSummary("objectType", before.typeDisplay(), formatLookup(after.getObjectType()))
            );
        }

        if (!equalsSafe(before.classId(), extractId(after.getObjectClass()))) {
            auditService.logFieldChange(
                    version,
                    "objectClass",
                    before.classDisplay(),
                    formatLookup(after.getObjectClass()),
                    actorId,
                    buildSummary("objectClass", before.classDisplay(), formatLookup(after.getObjectClass()))
            );
        }

        VaultEntity newVault = extractVault(after.getObjectType());
        if (!equalsSafe(before.vaultId(), extractId(newVault))) {
            auditService.logFieldChange(
                    version,
                    "vault",
                    before.vaultDisplay(),
                    formatLookup(newVault),
                    actorId,
                    buildSummary("vault", before.vaultDisplay(), formatLookup(newVault))
            );
        }
    }

    private boolean equalsSafe(Object left, Object right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    private Long extractId(ObjectType type) {
        return type != null ? type.getId() : null;
    }

    private Long extractId(ObjectClass objectClass) {
        return objectClass != null ? objectClass.getId() : null;
    }

    private Long extractId(VaultEntity vault) {
        return vault != null ? vault.getId() : null;
    }

    private String formatLookup(ObjectType type) {
        if (type == null) {
            return null;
        }
        return formatDisplay(type.getName(), type.getId());
    }

    private String formatLookup(ObjectClass objectClass) {
        if (objectClass == null) {
            return null;
        }
        return formatDisplay(objectClass.getName(), objectClass.getId());
    }

    private String formatLookup(VaultEntity vault) {
        if (vault == null) {
            return null;
        }
        return formatDisplay(vault.getName(), vault.getId());
    }

    private String formatDisplay(String name, Long id) {
        StringBuilder builder = new StringBuilder();
        if (name != null && !name.isBlank()) {
            builder.append(name.trim());
        }
        if (id != null) {
            if (!builder.isEmpty()) {
                builder.append(" (ID=").append(id).append(')');
            } else {
                builder.append("ID=").append(id);
            }
        }
        return builder.isEmpty() ? null : builder.toString();
    }

    private String buildSummary(String field,
                                String oldValue,
                                String newValue) {
        return String.format("Field '%s' changed from %s to %s",
                field,
                oldValue != null ? "'" + oldValue + "'" : "<null>",
                newValue != null ? "'" + newValue + "'" : "<null>");
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ObjectEntity> getAllFiltered(org.springframework.data.domain.Pageable pageable, Long typeId, Long classId, String search, boolean showDeleted) {
        org.springframework.data.jpa.domain.Specification<ObjectEntity> spec = org.springframework.data.jpa.domain.Specification.where(null);

        if (!showDeleted) {
            spec = spec.and((root, query, cb) -> cb.isFalse(root.get("isDeleted")));
        }
        if (typeId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("objectType").get("id"), typeId));
        }
        if (classId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("objectClass").get("id"), classId));
        }
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.toLowerCase().trim() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern));
        }

        Page<ObjectEntity> page = objectRepository.findAll(spec, pageable);
        page.forEach(this::initializeForRead);
        return page;
    }

    private record ObjectStateSnapshot(String name,
                                       Long typeId,
                                       String typeDisplay,
                                       Long classId,
                                       String classDisplay,
                                       Long vaultId,
                                       String vaultDisplay) {

        static ObjectStateSnapshot from(ObjectEntity entity) {
            if (entity == null) {
                return null;
            }

            ObjectType type = entity.getObjectType();
            ObjectClass objectClass = entity.getObjectClass();
            VaultEntity vault = type != null ? type.getVault() : null;

            return new ObjectStateSnapshot(
                    entity.getName(),
                    type != null ? type.getId() : null,
                    type != null ? safeLookupLabel(type.getName(), type.getId()) : null,
                    objectClass != null ? objectClass.getId() : null,
                    objectClass != null ? safeLookupLabel(objectClass.getName(), objectClass.getId()) : null,
                    vault != null ? vault.getId() : null,
                    vault != null ? safeLookupLabel(vault.getName(), vault.getId()) : null
            );
        }

        private static String safeLookupLabel(String name, Long id) {
            StringBuilder builder = new StringBuilder();
            if (name != null && !name.isBlank()) {
                builder.append(name.trim());
            }
            if (id != null) {
                if (!builder.isEmpty()) {
                    builder.append(" (ID=").append(id).append(')');
                } else {
                    builder.append("ID=").append(id);
                }
            }
            return builder.isEmpty() ? null : builder.toString();
        }
    }

    private void initializeForRead(ObjectEntity entity) {
        Hibernate.initialize(entity.getVersions());
        List<ObjectVersionEntity> versions = entity.getVersions();
        if (versions != null && !versions.isEmpty()) {
            ObjectVersionEntity firstVersion = versions.get(0);
            if (firstVersion != null && firstVersion.getCreatedBy() != null) {
                Hibernate.initialize(firstVersion.getCreatedBy());
            }
        }
    }

    private ObjectType resolveType(Long typeId) {
        if (typeId == null) {
            throw new IllegalArgumentException("Object type must be provided");
        }
        return objectTypeRepository.findById(typeId)
                .orElseThrow(() -> new EntityNotFoundException("Object type not found: " + typeId));
    }

    private ObjectClass resolveClass(Long classId) {
        if (classId == null) {
            return null;
        }
        return objectClassRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("Object class not found: " + classId));
    }

    private void ensureVaultConsistency(Long requestedVaultId, ObjectType type) {
        VaultEntity vault = extractVault(type);
        if (vault == null) {
            throw new IllegalStateException("Object type has no vault configured: " + (type != null ? type.getId() : null));
        }
        if (requestedVaultId != null && !equalsSafe(requestedVaultId, vault.getId())) {
            throw new IllegalArgumentException(String.format(
                    "Vault mismatch: type %d belongs to vault %d",
                    type.getId(),
                    vault.getId()
            ));
        }
    }

    private VaultEntity extractVault(ObjectType type) {
        return type != null ? type.getVault() : null;
    }

    @Deprecated
    @Transactional
    public void setValue(ObjectEntity object, String propertyName, Object value) {
        log.warn("⚠️ Deprecated setValue(String,Object) called for '{}'. Use setValue(Object, PropertyDef, Object).", propertyName);
    }


    @Transactional
    public void setValue(ObjectEntity object, PropertyDef def, Object value) {
        if (def == null) throw new IllegalArgumentException("PropertyDef is null");
        List<PropertyValue> dups = propertyValueRepository
                .findAllByObjectVersion_Object_IdAndPropertyDef_Id(object.getId(), def.getId());

        PropertyValue pv = dups.isEmpty() ? new PropertyValue() : dups.get(0);
        if (dups.size() > 1) {
            for (int i = 1; i < dups.size(); i++) propertyValueRepository.delete(dups.get(i));
        }

        pv.setPropertyDef(def);
        pv.setObjectVersion(objectVersionService.getOrCreateLatestVersion(object.getId()));

        // очищаем старые значения
        pv.setValueText(null);
        pv.setValueNumber(null);
        pv.setValueDate(null);
        pv.setValueBoolean(null);
        pv.setRefObject(null);
        pv.setValueListItem(null);

        switch (def.getDataType()) {
            case BOOLEAN -> pv.setValueBoolean(Boolean.parseBoolean(String.valueOf(value)));
            case NUMBER -> pv.setValueNumber(new BigDecimal(String.valueOf(value).replace(',', '.')));
            case DATE -> pv.setValueDate(LocalDate.parse(String.valueOf(value)).atStartOfDay());
            case VALUELIST -> {
                if (value instanceof Long id) {
                    // If not found, create new ValueListItem with placeholder name
                    ValueListItem item = valueListItemRepository.findById(id).orElseGet(() -> {
                        ValueListItem newItem = new ValueListItem();
                        newItem.setId(id); // Set the ID to match import if possible (may need to adjust if ID is generated)
                        newItem.setValue("Imported item " + id);
                        newItem.setIsActive(true);
                        return valueListItemRepository.save(newItem);
                    });
                    pv.setValueListItem(item);
                } else {
                    throw new IllegalArgumentException("Invalid valuelist id: " + value);
                }
            }
            default -> pv.setValueText(value != null ? value.toString() : null);
        }

        propertyValueRepository.save(pv);
        log.debug("💾 Установлено значение '{}' для '{}'", value, def.getName());
    }


    @Transactional
    public void setValueMulti(ObjectEntity object, PropertyDef def, List<Long> valueIds) {
        if (def == null) throw new IllegalArgumentException("PropertyDef is null");

        ObjectVersionEntity version = objectVersionService.getOrCreateLatestVersion(object.getId());
        List<PropertyValue> existing = propertyValueRepository.findAllByObjectVersionIdAndPropertyDefId(version.getId(), def.getId());

        PropertyValue pv = existing.isEmpty() ? new PropertyValue() : existing.get(0);
        if (existing.size() > 1) {
            for (int i = 1; i < existing.size(); i++) propertyValueRepository.delete(existing.get(i));
        }

        pv.setObjectVersion(version);
        pv.setPropertyDef(def);
        propertyValueRepository.save(pv);

        propertyValueMultiRepository.deleteAllByPropertyValueId(pv.getId());

        if (valueIds != null) {
            for (Long id : valueIds) {
                // If not found, create new ValueListItem with placeholder name
                ValueListItem item = valueListItemRepository.findById(id).orElseGet(() -> {
                    ValueListItem newItem = new ValueListItem();
                    newItem.setId(id); // Set the ID to match import if possible (may need to adjust if ID is generated)
                    newItem.setValue("Imported item " + id);
                    newItem.setIsActive(true);
                    return valueListItemRepository.save(newItem);
                });
                PropertyValueMulti multi = new PropertyValueMulti();
                multi.setPropertyValue(pv);
                multi.setValueListItem(item);
                propertyValueMultiRepository.save(multi);
            }
        }
        log.debug("💾 Мульти-значение {} установлено для '{}'", valueIds, def.getName());
    }


    @Transactional(readOnly = true)
    public Optional<ObjectEntity> findByTypeClassAndName(Long typeId, Long classId, String name) {
        if (typeId == null || classId == null || name == null || name.isBlank()) {
            return Optional.empty();
        }
        return objectRepository.findByObjectType_IdAndObjectClass_IdAndNameIgnoreCaseAndIsDeletedFalse(
                typeId,
                classId,
                name.trim()
        );
    }

}