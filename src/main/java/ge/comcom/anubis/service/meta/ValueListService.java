package ge.comcom.anubis.service.meta;

import ge.comcom.anubis.dto.ValueListDto;
import ge.comcom.anubis.entity.core.ValueList;
import ge.comcom.anubis.mapper.meta.ValueListMapper;
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
    private final ValueListMapper mapper;

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
}
