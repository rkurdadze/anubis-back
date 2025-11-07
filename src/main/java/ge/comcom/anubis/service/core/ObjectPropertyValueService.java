package ge.comcom.anubis.service.core;

import ge.comcom.anubis.dto.PropertyValueDto;
import ge.comcom.anubis.entity.core.ObjectEntity;
import ge.comcom.anubis.entity.core.ObjectVersionEntity;
import ge.comcom.anubis.entity.core.PropertyValue;
import ge.comcom.anubis.entity.core.PropertyValueMulti;
import ge.comcom.anubis.entity.core.ValueListItem;
import ge.comcom.anubis.entity.meta.PropertyDef;
import ge.comcom.anubis.enums.PropertyDataType;
import ge.comcom.anubis.repository.core.ObjectRepository;
import ge.comcom.anubis.repository.core.PropertyValueMultiRepository;
import ge.comcom.anubis.repository.meta.PropertyDefRepository;
import ge.comcom.anubis.repository.meta.PropertyValueRepository;
import ge.comcom.anubis.repository.meta.ValueListItemRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ObjectPropertyValueService {

    private final PropertyValueRepository propertyValueRepository;
    private final PropertyValueMultiRepository propertyValueMultiRepository;
    private final PropertyDefRepository propertyDefRepository;
    private final ValueListItemRepository valueListItemRepository;
    private final ObjectRepository objectRepository;
    private final ObjectVersionService versionService;
    private final ObjectVersionAuditService auditService;

    @Transactional
    public void savePropertyValues(Long versionId, List<PropertyValueDto> properties) {
        List<PropertyValueDto> incoming = properties != null ? properties : Collections.emptyList();

        ObjectVersionEntity baseVersion = versionService.getById(versionId);
        Map<Long, ExistingPropertySnapshot> existingSnapshots = loadExistingSnapshots(versionId);
        Map<Long, PropertyValueState> preparedStates = prepareIncomingStates(incoming);
        List<PropertyChangeRecord> changes = detectChanges(existingSnapshots, preparedStates);

        if (changes.isEmpty()) {
            return;
        }

        ObjectVersionEntity newVersion = versionService.createNewVersion(
                baseVersion.getObject().getId(),
                buildVersionComment(changes)
        );

        Set<Long> propertyIdsToKeep = new HashSet<>(preparedStates.keySet());
        removeUnmentionedProperties(newVersion, propertyIdsToKeep);
        persistPreparedStates(newVersion.getId(), preparedStates.values());
        logPropertyChanges(newVersion, changes);
    }

    public List<PropertyValueDto> getPropertyValues(Long versionId) {
        List<PropertyValueDto> result = new ArrayList<>();

        List<PropertyValue> properties = propertyValueRepository.findAllByObjectVersionId(versionId);

        for (PropertyValue pv : properties) {
            PropertyDef def = pv.getPropertyDef();

            if (Boolean.TRUE.equals(def.getIsMultiselect())) {
                List<Long> values = propertyValueMultiRepository
                        .findAllByPropertyValueId(pv.getId())
                        .stream()
                        .map(m -> m.getValueListItem().getId())
                        .toList();

                result.add(PropertyValueDto.builder()
                        .propertyDefId(def.getId())
                        .value(values)
                        .build());
            }
            else if (PropertyDataType.VALUELIST.equals(def.getDataType()) && pv.getValueListItem() != null) {
                result.add(PropertyValueDto.builder()
                        .propertyDefId(def.getId())
                        .value(pv.getValueListItem().getId())
                        .build());
            } else if (PropertyDataType.LOOKUP.equals(def.getDataType()) && pv.getRefObject() != null) {
                result.add(PropertyValueDto.builder()
                        .propertyDefId(def.getId())
                        .value(pv.getRefObject().getId())
                        .build());
            } else {
                result.add(PropertyValueDto.builder()
                        .propertyDefId(def.getId())
                        .value(resolveSimpleValue(pv))
                        .build());
            }
        }

        return result;
    }

    public void deletePropertyValues(Long versionId) {
        List<PropertyValue> props = propertyValueRepository.findAllByObjectVersionId(versionId);

        for (PropertyValue pv : props) {
            propertyValueMultiRepository.deleteAllByPropertyValueId(pv.getId());
        }

        propertyValueRepository.deleteAll(props);
    }

    private Map<Long, ExistingPropertySnapshot> loadExistingSnapshots(Long versionId) {
        List<PropertyValue> existingValues = propertyValueRepository.findAllByObjectVersionId(versionId);
        Map<Long, ExistingPropertySnapshot> snapshots = new HashMap<>();

        for (PropertyValue value : existingValues) {
            PropertyDef def = value.getPropertyDef();
            Object comparable;
            String audit;

            if (Boolean.TRUE.equals(def.getIsMultiselect())) {
                List<ValueListItem> items = propertyValueMultiRepository.findAllByPropertyValueId(value.getId())
                        .stream()
                        .map(PropertyValueMulti::getValueListItem)
                        .filter(Objects::nonNull)
                        .sorted(Comparator.comparing(ValueListItem::getId))
                        .toList();
                comparable = items.stream().map(ValueListItem::getId).toList();
                audit = formatMultiAudit(items);
            } else {
                switch (def.getDataType()) {
                    case VALUELIST -> {
                        ValueListItem item = value.getValueListItem();
                        comparable = item != null ? item.getId() : null;
                        audit = formatValueListItem(item);
                    }
                    case TEXT -> {
                        String text = value.getValueText();
                        comparable = text;
                        audit = text;
                    }
                    case NUMBER -> {
                        BigDecimal number = value.getValueNumber();
                        comparable = normalizeNumber(number);
                        audit = formatNumber(number);
                    }
                    case DATE -> {
                        LocalDateTime date = value.getValueDate();
                        comparable = date;
                        audit = date != null ? date.toString() : null;
                    }
                    case BOOLEAN -> {
                        Boolean bool = value.getValueBoolean();
                        comparable = bool;
                        audit = bool != null ? bool.toString() : null;
                    }
                    case LOOKUP -> {
                        ObjectEntity ref = value.getRefObject();
                        comparable = ref != null ? ref.getId() : null;
                        audit = formatLookup(ref);
                    }
                    default -> throw new IllegalStateException("Unsupported data type: " + def.getDataType());
                }
            }

            snapshots.put(def.getId(), new ExistingPropertySnapshot(def, comparable, audit));
        }

        return snapshots;
    }

    private Map<Long, PropertyValueState> prepareIncomingStates(List<PropertyValueDto> properties) {
        Map<Long, PropertyValueState> states = new HashMap<>();
        for (PropertyValueDto dto : properties) {
            if (dto.getPropertyDefId() == null) {
                continue;
            }

            PropertyDef def = propertyDefRepository.findById(dto.getPropertyDefId())
                    .orElseThrow(() -> new EntityNotFoundException("PropertyDef not found: " + dto.getPropertyDefId()));

            PropertyValueState state = new PropertyValueState(def);
            Object rawValue = dto.getValue();

            if (Boolean.TRUE.equals(def.getIsMultiselect())) {
                List<ValueListItem> items = resolveMultiValueItems(def, rawValue);
                state.setMultiValues(items);
                state.setComparableValue(items.stream().map(ValueListItem::getId).sorted().toList());
                state.setAuditValue(formatMultiAudit(items));
            } else {
                if (rawValue == null) {
                    state.setComparableValue(null);
                    state.setAuditValue(null);
                } else if (rawValue instanceof String str && str.trim().isEmpty()) {
                    state.setComparableValue(null);
                    state.setAuditValue(null);
                } else {
                    switch (def.getDataType()) {
                        case VALUELIST -> {
                            ValueListItem item = resolveValueListItem(def, rawValue);
                            state.setValueListItem(item);
                            state.setComparableValue(item != null ? item.getId() : null);
                            state.setAuditValue(formatValueListItem(item));
                        }
                        case TEXT -> {
                            String text = rawValue.toString();
                            state.setTextValue(text);
                            state.setComparableValue(text);
                            state.setAuditValue(text);
                        }
                        case NUMBER -> {
                            BigDecimal number = toBigDecimal(rawValue);
                            state.setNumberValue(number);
                            state.setComparableValue(normalizeNumber(number));
                            state.setAuditValue(formatNumber(number));
                        }
                        case DATE -> {
                            LocalDateTime date = toLocalDateTime(rawValue);
                            state.setDateValue(date);
                            state.setComparableValue(date);
                            state.setAuditValue(date != null ? date.toString() : null);
                        }
                        case BOOLEAN -> {
                            Boolean bool = toBoolean(rawValue);
                            state.setBooleanValue(bool);
                            state.setComparableValue(bool);
                            state.setAuditValue(bool != null ? bool.toString() : null);
                        }
                        case LOOKUP -> {
                            ObjectEntity ref = resolveLookupTarget(rawValue);
                            state.setLookupTarget(ref);
                            state.setComparableValue(ref != null ? ref.getId() : null);
                            state.setAuditValue(formatLookup(ref));
                        }
                        default -> throw new IllegalStateException("Unsupported data type: " + def.getDataType());
                    }
                }
            }

            states.put(def.getId(), state);
        }

        return states;
    }

    private List<PropertyChangeRecord> detectChanges(Map<Long, ExistingPropertySnapshot> existing,
                                                     Map<Long, PropertyValueState> prepared) {
        Set<Long> allKeys = new HashSet<>(existing.keySet());
        allKeys.addAll(prepared.keySet());

        List<PropertyChangeRecord> changes = new ArrayList<>();

        for (Long defId : allKeys) {
            ExistingPropertySnapshot before = existing.get(defId);
            PropertyValueState after = prepared.get(defId);

            Object beforeComparable = before != null ? before.comparableValue() : null;
            Object afterComparable = after != null ? after.getComparableValue() : null;

            if (!Objects.equals(beforeComparable, afterComparable)) {
                PropertyDef def = before != null ? before.def() : (after != null ? after.getDef() : null);
                String oldValue = before != null ? before.auditValue() : null;
                String newValue = after != null ? after.getAuditValue() : null;
                if (def != null) {
                    changes.add(new PropertyChangeRecord(def, oldValue, newValue));
                }
            }
        }

        return changes;
    }

    private void persistPreparedStates(Long versionId, Collection<PropertyValueState> states) {
        for (PropertyValueState state : states) {
            if (Boolean.TRUE.equals(state.getDef().getIsMultiselect())) {
                applyMultiState(versionId, state);
            } else {
                applySingleState(versionId, state);
            }
        }
    }

    private void removeUnmentionedProperties(ObjectVersionEntity version, Set<Long> propertyIdsToKeep) {
        List<PropertyValue> clonedValues = propertyValueRepository.findAllByObjectVersionId(version.getId());
        if (clonedValues.isEmpty()) {
            return;
        }

        List<PropertyValue> toRemove = new ArrayList<>();
        for (PropertyValue value : clonedValues) {
            Long propertyDefId = value.getPropertyDef() != null ? value.getPropertyDef().getId() : null;
            if (propertyDefId == null || !propertyIdsToKeep.contains(propertyDefId)) {
                if (value.getId() != null) {
                    propertyValueMultiRepository.deleteAllByPropertyValueId(value.getId());
                }
                toRemove.add(value);
            }
        }

        if (!toRemove.isEmpty()) {
            propertyValueRepository.deleteAll(toRemove);
            if (version.getPropertyValues() != null) {
                Set<Long> removedDefIds = toRemove.stream()
                        .map(PropertyValue::getPropertyDef)
                        .filter(Objects::nonNull)
                        .map(PropertyDef::getId)
                        .collect(Collectors.toSet());
                version.getPropertyValues().removeIf(pv -> pv.getPropertyDef() != null
                        && removedDefIds.contains(pv.getPropertyDef().getId()));
            }
        }
    }

    private void applySingleState(Long versionId, PropertyValueState state) {
        PropertyValue pv = getOrCreateBaseValue(versionId, state.getDef());

        if (pv.getId() != null) {
            propertyValueMultiRepository.deleteAllByPropertyValueId(pv.getId());
        }

        pv.setValueListItem(null);
        pv.setValueText(null);
        pv.setValueNumber(null);
        pv.setValueDate(null);
        pv.setValueBoolean(null);
        pv.setRefObject(null);

        if (state.getValueListItem() != null) {
            pv.setValueListItem(state.getValueListItem());
        } else if (state.getTextValue() != null) {
            pv.setValueText(state.getTextValue());
        } else if (state.getNumberValue() != null) {
            pv.setValueNumber(state.getNumberValue());
        } else if (state.getDateValue() != null) {
            pv.setValueDate(state.getDateValue());
        } else if (state.getBooleanValue() != null) {
            pv.setValueBoolean(state.getBooleanValue());
        } else if (state.getLookupTarget() != null) {
            pv.setRefObject(state.getLookupTarget());
        }

        propertyValueRepository.save(pv);
    }

    private void applyMultiState(Long versionId, PropertyValueState state) {
        PropertyValue pv = getOrCreateBaseValue(versionId, state.getDef());
        pv.setValueListItem(null);
        pv.setValueText(null);
        pv.setValueNumber(null);
        pv.setValueDate(null);
        pv.setValueBoolean(null);
        pv.setRefObject(null);
        pv = propertyValueRepository.save(pv);

        if (pv.getId() != null) {
            propertyValueMultiRepository.deleteAllByPropertyValueId(pv.getId());
        }

        for (ValueListItem item : state.getMultiValues()) {
            if (item == null) {
                continue;
            }
            PropertyValueMulti multi = new PropertyValueMulti();
            multi.setPropertyValue(pv);
            multi.setValueListItem(item);
            propertyValueMultiRepository.save(multi);
        }
    }

    private void logPropertyChanges(ObjectVersionEntity version, List<PropertyChangeRecord> changes) {
        if (changes.isEmpty()) {
            return;
        }

        Long actorId = version.getCreatedBy() != null ? version.getCreatedBy().getId() : null;

        for (PropertyChangeRecord change : changes) {
            String fieldName = change.def().getName();
            String summary = buildSummary(fieldName, change.oldValue(), change.newValue());
            auditService.logFieldChange(
                    version,
                    fieldName,
                    change.oldValue(),
                    change.newValue(),
                    actorId,
                    summary
            );
        }
    }

    private String buildSummary(String propertyName, String oldValue, String newValue) {
        return String.format("Property '%s' changed from %s to %s",
                propertyName,
                oldValue != null ? "'" + oldValue + "'" : "<null>",
                newValue != null ? "'" + newValue + "'" : "<null>");
    }

    private String buildVersionComment(List<PropertyChangeRecord> changes) {
        if (changes.isEmpty()) {
            return "Properties updated";
        }
        if (changes.size() == 1) {
            return "Property '" + changes.get(0).def().getName() + "' updated";
        }
        return changes.size() + " properties updated";
    }

    private String formatMultiAudit(List<ValueListItem> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        return items.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing(ValueListItem::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ValueListItem::getId))
                .map(this::formatValueListItem)
                .collect(Collectors.joining(", "));
    }

    private String formatValueListItem(ValueListItem item) {
        if (item == null) {
            return null;
        }
        String value = item.getValue();
        if (value != null && !value.isBlank()) {
            return item.getId() != null
                    ? value + " (ID=" + item.getId() + ")"
                    : value;
        }
        return item.getId() != null ? "ID=" + item.getId() : null;
    }

    private String formatLookup(ObjectEntity ref) {
        if (ref == null) {
            return null;
        }
        String name = ref.getName();
        if (name != null && !name.isBlank()) {
            return ref.getId() != null
                    ? name + " (ID=" + ref.getId() + ")"
                    : name;
        }
        return ref.getId() != null ? "ID=" + ref.getId() : null;
    }

    private List<ValueListItem> resolveMultiValueItems(PropertyDef def, Object raw) {
        if (raw == null) {
            return List.of();
        }

        List<?> values = raw instanceof List<?> list ? list : List.of(raw);
        Map<Long, ValueListItem> deduplicated = new HashMap<>();
        List<ValueListItem> ordered = new ArrayList<>();

        for (Object element : values) {
            if (element == null) {
                continue;
            }
            ValueListItem item = resolveValueListItem(def, element);
            if (item == null) {
                continue;
            }
            if (!deduplicated.containsKey(item.getId())) {
                deduplicated.put(item.getId(), item);
                ordered.add(item);
            }
        }

        return ordered;
    }

    private Object resolveSimpleValue(PropertyValue pv) {
        if (pv.getValueText() != null) {
            return pv.getValueText();
        }
        if (pv.getValueNumber() != null) {
            return pv.getValueNumber();
        }
        if (pv.getValueDate() != null) {
            return pv.getValueDate();
        }
        if (pv.getValueBoolean() != null) {
            return pv.getValueBoolean();
        }
        return null;
    }

    private BigDecimal toBigDecimal(Object raw) {
        if (raw instanceof BigDecimal bd) {
            return bd;
        }
        if (raw instanceof Number num) {
            return new BigDecimal(num.toString());
        }
        if (raw instanceof String str) {
            String trimmed = str.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            return new BigDecimal(trimmed);
        }
        throw new IllegalArgumentException("Unsupported numeric value type: " + raw.getClass());
    }

    private LocalDateTime toLocalDateTime(Object raw) {
        if (raw instanceof LocalDateTime ldt) {
            return ldt;
        }
        if (raw instanceof OffsetDateTime odt) {
            return odt.toLocalDateTime();
        }
        if (raw instanceof LocalDate ld) {
            return ld.atStartOfDay();
        }
        if (raw instanceof String str) {
            String trimmed = str.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            try {
                return OffsetDateTime.parse(trimmed).toLocalDateTime();
            } catch (Exception ignored) {
            }
            try {
                return LocalDateTime.parse(trimmed);
            } catch (Exception ignored) {
            }
            return LocalDate.parse(trimmed).atStartOfDay();
        }
        throw new IllegalArgumentException("Unsupported date value type: " + raw.getClass());
    }

    private Boolean toBoolean(Object raw) {
        if (raw instanceof Boolean b) {
            return b;
        }
        if (raw instanceof Number num) {
            return num.intValue() != 0;
        }
        if (raw instanceof String str) {
            String normalized = str.trim().toLowerCase();
            if (normalized.isEmpty()) {
                return null;
            }
            if (normalized.equals("true") || normalized.equals("1")) {
                return Boolean.TRUE;
            }
            if (normalized.equals("false") || normalized.equals("0")) {
                return Boolean.FALSE;
            }
        }
        throw new IllegalArgumentException("Unsupported boolean value: " + raw);
    }

    private ObjectEntity resolveLookupTarget(Object raw) {
        Long objectId = extractLong(raw);
        if (objectId == null) {
            return null;
        }
        return objectRepository.findById(objectId)
                .orElseThrow(() -> new EntityNotFoundException("Object not found: " + objectId));
    }

    private Long extractLong(Object raw) {
        if (raw instanceof Number num) {
            return num.longValue();
        }
        if (raw instanceof String str) {
            String trimmed = str.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            return Long.parseLong(trimmed);
        }
        throw new IllegalArgumentException("Unsupported identifier type: " + raw.getClass());
    }

    private PropertyValue getOrCreateBaseValue(Long versionId, PropertyDef def) {
        return propertyValueRepository
                .findByObjectVersionIdAndPropertyDefId(versionId, def.getId())
                .orElseGet(() -> {
                    PropertyValue pv = new PropertyValue();
                    ObjectVersionEntity versionRef = new ObjectVersionEntity();
                    versionRef.setId(versionId);
                    pv.setObjectVersion(versionRef);
                    pv.setPropertyDef(def);
                    return pv;
                });
    }

    private ValueListItem resolveValueListItem(PropertyDef def, Object rawValue) {
        if (rawValue instanceof Number num) {
            return valueListItemRepository.findById(num.longValue())
                    .orElseThrow(() -> new EntityNotFoundException("ValueListItem not found: " + num));
        }
        if (rawValue instanceof String str) {
            String trimmed = str.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            if (def.getValueList() == null) {
                throw new IllegalStateException("No ValueList for property " + def.getName());
            }

            try {
                long id = Long.parseLong(trimmed);
                return valueListItemRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("ValueListItem not found: " + id));
            } catch (NumberFormatException ignore) {
            }

            return valueListItemRepository
                    .findAllByValueListIdOrderBySortOrderAsc(def.getValueList().getId())
                    .stream()
                    .filter(i -> i.getValue().equalsIgnoreCase(trimmed))
                    .findFirst()
                    .orElseThrow(() -> new EntityNotFoundException("No ValueListItem for text: " + str));
        }
        throw new IllegalArgumentException("Unsupported ValueList value: " + rawValue.getClass());
    }

    private String formatNumber(BigDecimal number) {
        if (number == null) {
            return null;
        }
        if (number.compareTo(BigDecimal.ZERO) == 0) {
            return "0";
        }
        return number.stripTrailingZeros().toPlainString();
    }

    private BigDecimal normalizeNumber(BigDecimal number) {
        if (number == null) {
            return null;
        }
        if (number.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return number.stripTrailingZeros();
    }

    private record ExistingPropertySnapshot(PropertyDef def, Object comparableValue, String auditValue) {
    }

    private record PropertyChangeRecord(PropertyDef def, String oldValue, String newValue) {
    }

    private static class PropertyValueState {
        private final PropertyDef def;
        private String textValue;
        private BigDecimal numberValue;
        private LocalDateTime dateValue;
        private Boolean booleanValue;
        private ValueListItem valueListItem;
        private List<ValueListItem> multiValues = List.of();
        private ObjectEntity lookupTarget;
        private Object comparableValue;
        private String auditValue;

        private PropertyValueState(PropertyDef def) {
            this.def = def;
        }

        public PropertyDef getDef() {
            return def;
        }

        public String getTextValue() {
            return textValue;
        }

        public void setTextValue(String textValue) {
            this.textValue = textValue;
        }

        public BigDecimal getNumberValue() {
            return numberValue;
        }

        public void setNumberValue(BigDecimal numberValue) {
            this.numberValue = numberValue;
        }

        public LocalDateTime getDateValue() {
            return dateValue;
        }

        public void setDateValue(LocalDateTime dateValue) {
            this.dateValue = dateValue;
        }

        public Boolean getBooleanValue() {
            return booleanValue;
        }

        public void setBooleanValue(Boolean booleanValue) {
            this.booleanValue = booleanValue;
        }

        public ValueListItem getValueListItem() {
            return valueListItem;
        }

        public void setValueListItem(ValueListItem valueListItem) {
            this.valueListItem = valueListItem;
        }

        public List<ValueListItem> getMultiValues() {
            return multiValues;
        }

        public void setMultiValues(List<ValueListItem> multiValues) {
            this.multiValues = multiValues != null ? multiValues : List.of();
        }

        public ObjectEntity getLookupTarget() {
            return lookupTarget;
        }

        public void setLookupTarget(ObjectEntity lookupTarget) {
            this.lookupTarget = lookupTarget;
        }

        public Object getComparableValue() {
            return comparableValue;
        }

        public void setComparableValue(Object comparableValue) {
            this.comparableValue = comparableValue;
        }

        public String getAuditValue() {
            return auditValue;
        }

        public void setAuditValue(String auditValue) {
            this.auditValue = auditValue;
        }
    }
}
