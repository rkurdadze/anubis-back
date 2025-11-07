package ge.comcom.anubis.service.core;

import ge.comcom.anubis.entity.core.LinkRole;
import ge.comcom.anubis.enums.LinkDirection;
import ge.comcom.anubis.repository.core.LinkRoleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для управления ролями связей между объектами.
 * Инкапсулирует доступ к {@link LinkRoleRepository}, чтобы контроллеры
 * не зависели напрямую от слоя хранения данных.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class LinkRoleService {

    private final LinkRoleRepository repository;

    @Transactional(readOnly = true)
    public List<LinkRole> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<LinkRole> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<LinkRole> findByNameIgnoreCase(String name) {
        return repository.findByNameIgnoreCase(name);
    }

    public LinkRole create(LinkRole role) {
        if (role.getDirection() == null) {
            role.setDirection(LinkDirection.UNI);
        }
        if (role.getIsActive() == null) {
            role.setIsActive(Boolean.TRUE);
        }
        return repository.save(role);
    }

    public LinkRole update(Long id, LinkRole payload) {
        LinkRole existing = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Link role not found: " + id));

        existing.setName(payload.getName());
        existing.setNameI18n(payload.getNameI18n());
        existing.setDescription(payload.getDescription());
        existing.setDirection(payload.getDirection() != null ? payload.getDirection() : LinkDirection.UNI);
        existing.setIsActive(payload.getIsActive());
        return repository.save(existing);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Link role not found: " + id);
        }
        repository.deleteById(id);
    }
}
