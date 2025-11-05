package ge.comcom.anubis.service.core;

import ge.comcom.anubis.entity.core.ObjectType;
import ge.comcom.anubis.entity.core.VaultEntity;
import ge.comcom.anubis.repository.core.ObjectTypeRepository;
import ge.comcom.anubis.repository.core.VaultRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ObjectTypeService {

    private final ObjectTypeRepository objectTypeRepository;
    private final VaultRepository vaultRepository;

    @Transactional(readOnly = true)
    public List<ObjectType> findAll() {
        return objectTypeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ObjectType findById(Long id) {
        return objectTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ObjectType not found: " + id));
    }

    @Transactional
    public ObjectType create(ObjectType objectType) {
        if (objectType.getVault() == null || objectType.getVault().getId() == null) {
            throw new IllegalArgumentException("Vault ID must be provided");
        }

        VaultEntity vault = vaultRepository.findById(objectType.getVault().getId())
                .orElseThrow(() -> new EntityNotFoundException("Vault not found: " + objectType.getVault().getId()));

        objectType.setVault(vault);
        ObjectType saved = objectTypeRepository.save(objectType);
        return findById(saved.getId());
    }

    @Transactional
    public ObjectType update(Long id, ObjectType updated) {
        ObjectType existing = objectTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ObjectType not found: " + id));

        existing.setName(updated.getName());

        if (updated.getVault() != null && updated.getVault().getId() != null) {
            VaultEntity vault = vaultRepository.findById(updated.getVault().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Vault not found: " + updated.getVault().getId()));
            existing.setVault(vault);
        }

        ObjectType saved = objectTypeRepository.save(existing);
        return findById(saved.getId());
    }

    @Transactional
    public void delete(Long id) {
        if (!objectTypeRepository.existsById(id)) {
            throw new EntityNotFoundException("ObjectType not found: " + id);
        }
        objectTypeRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public ObjectType findByName(String name) {
        return objectTypeRepository.findByNameIgnoreCase(name).orElse(null);
    }

    @Transactional
    public ObjectType save(ObjectType objectType) {
        return objectTypeRepository.save(objectType);
    }
}
