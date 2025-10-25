package ge.comcom.anubis.service.meta;

import ge.comcom.anubis.dto.ValueListDto;
import ge.comcom.anubis.entity.core.ValueList;
import ge.comcom.anubis.repository.meta.ValueListRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ValueListService {

    private final ValueListRepository repository;

    public ValueListDto create(ValueListDto req) {
        if (repository.existsByNameIgnoreCase(req.getName())) {
            throw new IllegalArgumentException("ValueList already exists: " + req.getName());
        }
        ValueList entity = ValueList.builder()
                .name(req.getName().trim())
                .nameI18n(req.getNameI18n())
                .build();
        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Page<ValueListDto> list(Pageable pageable) {
        return repository.findAll(pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public ValueListDto get(Long id) {
        return toDto(repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ValueList not found: id=" + id)));
    }

    public ValueListDto update(Long id, ValueListDto req) {
        ValueList e = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ValueList not found: id=" + id));

        if (req.getName() != null && !req.getName().isBlank()) {
            String name = req.getName().trim();
            if (repository.existsByNameIgnoreCaseAndIdNot(name, id))
                throw new IllegalArgumentException("ValueList name already exists");
            e.setName(name);
        }

        if (req.getNameI18n() != null) e.setNameI18n(req.getNameI18n());

        return toDto(repository.save(e));
    }

    public void delete(Long id) {
        if (!repository.existsById(id))
            throw new EntityNotFoundException("ValueList not found: id=" + id);
        repository.deleteById(id);
    }

    private ValueListDto toDto(ValueList e) {
        return ValueListDto.builder()
                .id(e.getId())
                .name(e.getName())
                .nameI18n(e.getNameI18n())
                .build();
    }
}
