package ge.comcom.anubis.service.core;

import ge.comcom.anubis.dto.PropertyValueDto;
import ge.comcom.anubis.entity.core.*;
import ge.comcom.anubis.entity.meta.PropertyDef;
import ge.comcom.anubis.entity.core.PropertyValueMulti;
import ge.comcom.anubis.repository.meta.PropertyDefRepository;
import ge.comcom.anubis.repository.core.PropertyValueMultiRepository;
import ge.comcom.anubis.repository.meta.PropertyValueRepository;
import ge.comcom.anubis.repository.meta.ValueListItemRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        PropertyValue pv = new PropertyValue();
        ObjectVersion versionRef = new ObjectVersion();
        versionRef.setId(versionId);
        pv.setObjectVersion(versionRef);
        pv.setPropertyDef(def);

        if ("VALUELIST".equals(def.getDataType().name())) {
            if (dto.getValue() instanceof Number num) {
                ValueListItem item = valueListItemRepository.findById(num.longValue())
                        .orElseThrow(() -> new EntityNotFoundException("ValueListItem not found: " + num));
                pv.setValueListItem(item);
            } else if (dto.getValue() instanceof String str) {
                if (def.getValueList() == null) throw new IllegalStateException("No ValueList for property " + def.getName());
                ValueListItem item = valueListItemRepository
                        .findAllByValueListIdOrderBySortOrderAsc(def.getValueList().getId())
                        .stream()
                        .filter(i -> i.getValue().equalsIgnoreCase(str))
                        .findFirst()
                        .orElseThrow(() -> new EntityNotFoundException("No ValueListItem for text: " + str));
                pv.setValueListItem(item);
            }
        } else if (dto.getValue() != null) {
            pv.setValueText(dto.getValue().toString());
        }

        propertyValueRepository.save(pv);
    }

    private void saveMultiValue(Long versionId, PropertyDef def, PropertyValueDto dto) {
        PropertyValue pv = new PropertyValue();
        ObjectVersion versionRef = new ObjectVersion();
        versionRef.setId(versionId);
        pv.setObjectVersion(versionRef);
        pv.setPropertyDef(def);
        propertyValueRepository.save(pv);

        List<?> values = (dto.getValue() instanceof List)
                ? (List<?>) dto.getValue()
                : List.of(dto.getValue());

        for (Object v : values) {
            final Long itemId;

            if (v instanceof Number n) {
                itemId = n.longValue();
            } else if (v instanceof String s) {
                itemId = valueListItemRepository
                        .findAllByValueListIdOrderBySortOrderAsc(def.getValueList().getId())
                        .stream()
                        .filter(i -> i.getValue().equalsIgnoreCase(s))
                        .map(ValueListItem::getId)
                        .findFirst()
                        .orElseThrow(() -> new EntityNotFoundException("No ValueListItem for: " + s));
            } else {
                continue;
            }

            PropertyValueMulti m = new PropertyValueMulti();
            m.setPropertyValue(pv);
            m.setValueListItem(
                    valueListItemRepository.findById(itemId)
                            .orElseThrow(() -> new EntityNotFoundException("ValueListItem not found: " + itemId))
            );
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
                List<String> values = propertyValueMultiRepository
                        .findAllByPropertyValueId(pv.getId())
                        .stream()
                        .map(m -> m.getValueListItem().getValue())
                        .toList();

                result.add(PropertyValueDto.builder()
                        .propertyDefId(def.getId())
                        .value(values)
                        .build());
            }
            // === SINGLE VALUE ===
            else if ("VALUELIST".equals(def.getDataType().name()) && pv.getValueListItem() != null) {
                result.add(PropertyValueDto.builder()
                        .propertyDefId(def.getId())
                        .value(pv.getValueListItem().getValue())
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


}
