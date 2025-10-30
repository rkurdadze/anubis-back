package ge.comcom.anubis.service.core;

import ge.comcom.anubis.dto.PropertyValueDto;
import ge.comcom.anubis.entity.core.ObjectEntity;
import ge.comcom.anubis.entity.core.ObjectVersionEntity;
import ge.comcom.anubis.entity.core.PropertyValue;
import ge.comcom.anubis.entity.core.PropertyValueMulti;
import ge.comcom.anubis.entity.core.ValueListItem;
import ge.comcom.anubis.entity.meta.PropertyDef;
import ge.comcom.anubis.enums.PropertyDataType;
import ge.comcom.anubis.repository.meta.PropertyDefRepository;
import ge.comcom.anubis.repository.core.PropertyValueMultiRepository;
import ge.comcom.anubis.repository.meta.PropertyValueRepository;
import ge.comcom.anubis.repository.meta.ValueListItemRepository;
import ge.comcom.anubis.repository.core.ObjectRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ObjectPropertyValueService {

    private final PropertyValueRepository propertyValueRepository;
    private final PropertyValueMultiRepository propertyValueMultiRepository;
    private final PropertyDefRepository propertyDefRepository;
    private final ValueListItemRepository valueListItemRepository;
    private final ObjectRepository objectRepository;

    public void savePropertyValues(Long versionId, List<PropertyValueDto> properties) {
        for (PropertyValueDto dto : properties) {
            PropertyDef def = propertyDefRepository.findById(dto.getPropertyDefId())
                    .orElseThrow(() -> new EntityNotFoundException("PropertyDef not found: " + dto.getPropertyDefId()));

            if (Boolean.TRUE.equals(def.getIsMultiselect())) {
                saveMultiValue(versionId, def, dto);
            } else {
                saveSingleValue(versionId, def, dto);
            }
        }
    }

    private void saveSingleValue(Long versionId, PropertyDef def, PropertyValueDto dto) {
        PropertyValue pv = getOrCreateBaseValue(versionId, def);

        // single значение не должно иметь хвостов из multi-select
        if (pv.getId() != null) {
            propertyValueMultiRepository.deleteAllByPropertyValueId(pv.getId());
        }

        pv.setValueListItem(null);
        pv.setValueText(null);
        pv.setValueNumber(null);
        pv.setValueDate(null);
        pv.setValueBoolean(null);
        pv.setRefObject(null);

        Object rawValue = dto.getValue();

        if (rawValue == null) {
            propertyValueRepository.save(pv);
            return;
        }

        if (rawValue instanceof String str && str.trim().isEmpty()) {
            propertyValueRepository.save(pv);
            return;
        }

        if (def.getDataType() == null) {
            propertyValueRepository.save(pv);
            return;
        }

        switch (def.getDataType()) {
            case VALUELIST -> pv.setValueListItem(resolveValueListItem(def, rawValue));
            case TEXT -> pv.setValueText(rawValue.toString());
            case NUMBER -> pv.setValueNumber(toBigDecimal(rawValue));
            case DATE -> pv.setValueDate(toLocalDateTime(rawValue));
            case BOOLEAN -> pv.setValueBoolean(toBoolean(rawValue));
            case LOOKUP -> pv.setRefObject(resolveLookupTarget(rawValue));
        }

        propertyValueRepository.save(pv);
    }

    private void saveMultiValue(Long versionId, PropertyDef def, PropertyValueDto dto) {
        if (def.getDataType() != PropertyDataType.VALUELIST) {
            throw new IllegalStateException("Multiselect поддерживается только для ValueList");
        }

        PropertyValue pv = getOrCreateBaseValue(versionId, def);
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

        Object raw = dto.getValue();
        if (raw == null) {
            return;
        }

        List<?> values;
        if (raw instanceof List<?> list) {
            values = list;
        } else {
            values = raw == null ? List.of() : List.of(raw);
        }

        for (Object v : values) {
            if (v == null) {
                continue;
            }

            ValueListItem item = resolveValueListItem(def, v);
            if (item == null) {
                continue;
            }

            PropertyValueMulti m = new PropertyValueMulti();
            m.setPropertyValue(pv);
            m.setValueListItem(item);
            propertyValueMultiRepository.save(m);
        }
    }

    public List<PropertyValueDto> getPropertyValues(Long versionId) {
        List<PropertyValueDto> result = new ArrayList<>();

        // Загружаем все single и multi значения
        List<PropertyValue> properties = propertyValueRepository.findAllByObjectVersionId(versionId);

        for (PropertyValue pv : properties) {
            PropertyDef def = pv.getPropertyDef();

            // === MULTISELECT ===
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
            // === SINGLE VALUE ===
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

    /**
     * Удаляет все PropertyValue и связанные PropertyValueMulti для заданной версии объекта.
     */
    public void deletePropertyValues(Long versionId) {
        // Загружаем все значения по версии
        List<PropertyValue> props = propertyValueRepository.findAllByObjectVersionId(versionId);

        // Сначала удаляем все Multi-значения (иначе FK нарушится)
        for (PropertyValue pv : props) {
            propertyValueMultiRepository.deleteAllByPropertyValueId(pv.getId());
        }

        // Затем удаляем базовые PropertyValue
        propertyValueRepository.deleteAll(props);
    }


    /**
     * Возвращает простое значение свойства (TEXT, NUMBER, DATE, BOOLEAN).
     */
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
                // не число — ищем по тексту
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


}
