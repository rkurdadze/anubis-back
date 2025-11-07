package ge.comcom.anubis.service.meta;

import ge.comcom.anubis.dto.ValueListDto;
import ge.comcom.anubis.entity.core.ValueList;
import ge.comcom.anubis.entity.core.ValueListItem;
import ge.comcom.anubis.mapper.meta.ValueListMapper;
import ge.comcom.anubis.repository.meta.ValueListItemRepository;
import ge.comcom.anubis.repository.meta.ValueListRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ValueListService {

    private final ValueListRepository repository;
    private final ValueListMapper mapper;

    private final ValueListItemRepository itemRepository;

    public ValueListDto create(ValueListDto req) {
        String name = req.getName().trim();
        if (repository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("ValueList already exists: " + req.getName());
        }
        req.setName(name);
        ValueList entity = mapper.toEntity(req);
        return mapper.toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Page<ValueListDto> list(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public ValueListDto get(Long id) {
        return mapper.toDto(repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ValueList not found: id=" + id)));
    }

    public ValueListDto update(Long id, ValueListDto req) {
        ValueList e = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ValueList not found: id=" + id));

        if (req.getName() != null && !req.getName().isBlank()) {
            String name = req.getName().trim();
            if (repository.existsByNameIgnoreCaseAndIdNot(name, id))
                throw new IllegalArgumentException("ValueList name already exists");
            req.setName(name);
        }

        mapper.updateEntityFromDto(req, e);

        return mapper.toDto(repository.save(e));
    }

    public void delete(Long id) {
        if (!repository.existsById(id))
            throw new EntityNotFoundException("ValueList not found: id=" + id);
        repository.deleteById(id);
    }

    /**
     * Мягкое удаление ValueList (soft-delete)
     */
    public void deactivate(Long id) {
        ValueList e = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ValueList not found: id=" + id));
        repository.deactivate(e);
    }




    public ValueList upsertByName(String name) {
        String normalized = name.trim();
        return repository.findByNameIgnoreCase(normalized)
                .orElseGet(() -> {
                    ValueList created = new ValueList();
                    created.setName(normalized);
                    ValueList saved = repository.save(created);
                    log.info("🆕 Создан ValueList '{}'", normalized);
                    return saved;
                });
    }

    /**
     * Ищет элемент в ValueList по имени (без регистра), если не найден — создаёт новый.
     */
    public ValueListItem upsertItem(Long valueListId, String itemName) {
        if (valueListId == null) {
            throw new IllegalArgumentException("ValueList id must be provided");
        }

        if (itemName == null || itemName.isBlank()) {
            throw new IllegalArgumentException("ValueList item name must be provided");
        }

        ValueList valueList = repository.findById(valueListId)
                .orElseThrow(() -> new EntityNotFoundException("ValueList not found: id=" + valueListId));

        String normalized = itemName.trim();
        Optional<ValueListItem> existing = itemRepository.findByValueListAndValueIgnoreCase(valueList, normalized);
        if (existing.isPresent()) {
            return existing.get();
        }

        ValueListItem created = new ValueListItem();
        created.setValueList(valueList);
        created.setValue(normalized);
        ValueListItem saved = itemRepository.save(created);
        log.info("🆕 Создан ValueListItem '{}' в списке '{}'", normalized, valueList.getName());
        return saved;
    }
}
