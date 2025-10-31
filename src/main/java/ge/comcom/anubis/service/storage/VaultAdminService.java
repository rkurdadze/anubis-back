package ge.comcom.anubis.service.storage;

import ge.comcom.anubis.dto.VaultDto;
import ge.comcom.anubis.dto.VaultRequest;
import ge.comcom.anubis.entity.core.FileStorageEntity;
import ge.comcom.anubis.entity.core.VaultEntity;
import ge.comcom.anubis.mapper.VaultMapper;
import ge.comcom.anubis.repository.core.FileStorageRepository;
import ge.comcom.anubis.repository.core.ObjectTypeRepository;
import ge.comcom.anubis.repository.core.VaultRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Сервис управления логическими vault'ами.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VaultAdminService {

    private final VaultRepository vaultRepository;
    private final FileStorageRepository fileStorageRepository;
    private final ObjectTypeRepository objectTypeRepository;
    private final VaultMapper vaultMapper;

    public List<VaultDto> findAll() {
        return vaultRepository.findAll().stream()
                .map(this::mapWithStorage)
                .toList();
    }

    public VaultDto findById(Long id) {
        VaultEntity entity = vaultRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Vault not found: " + id));
        return mapWithStorage(entity);
    }

    @Transactional
    public VaultDto create(VaultRequest request) {
        validateUniqueness(request.getCode(), null);

        VaultEntity entity = new VaultEntity();
        applyRequest(entity, request);

        return mapWithStorage(vaultRepository.save(entity));
    }

    @Transactional
    public VaultDto update(Long id, VaultRequest request) {
        VaultEntity entity = vaultRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Vault not found: " + id));
        validateUniqueness(request.getCode(), id);

        applyRequest(entity, request);

        return mapWithStorage(vaultRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        VaultEntity entity = vaultRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Vault not found: " + id));
        if (objectTypeRepository.existsByVault_Id(id)) {
            throw new IllegalStateException("Cannot delete vault that still contains objects");
        }
        vaultRepository.delete(entity);
    }

    private void applyRequest(VaultEntity entity, VaultRequest request) {
        entity.setCode(request.getCode().trim());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());

        // Используем корректный геттер
        boolean requestedActive = request.isActive();
        entity.setActive(requestedActive);

        if (request.getDefaultStorageId() != null) {
            FileStorageEntity storage = fileStorageRepository.findById(request.getDefaultStorageId())
                    .orElseThrow(() -> new IllegalArgumentException("File storage not found: " + request.getDefaultStorageId()));
            if (!storage.isActive()) {
                throw new IllegalStateException("Default storage must be active");
            }
            entity.setDefaultStorage(storage);
        } else {
            entity.setDefaultStorage(null);
        }
    }


    private void validateUniqueness(String code, Long currentId) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("Vault code cannot be empty");
        }
        boolean exists;
        if (currentId == null) {
            exists = vaultRepository.existsByCodeIgnoreCase(code);
        } else {
            exists = vaultRepository.existsByCodeIgnoreCaseAndIdNot(code, currentId);
        }
        if (exists) {
            throw new IllegalStateException("Vault with code '" + code + "' already exists");
        }
    }

    private VaultDto mapWithStorage(VaultEntity entity) {
        if (entity.getDefaultStorage() != null) {
            entity.getDefaultStorage().getName(); // инициализируем proxy
        }
        return vaultMapper.toDto(entity);
    }
}
