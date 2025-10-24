package ge.comcom.anubis.service.core;

import ge.comcom.anubis.entity.core.ObjectEntity;
import ge.comcom.anubis.repository.core.ObjectRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Service layer for managing repository objects.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ObjectService {

    private final ObjectRepository objectRepository;

    public ObjectEntity create(ObjectEntity entity) {
        entity.setCreatedAt(Instant.now());
        log.info("Creating new object '{}'", entity.getName());
        return objectRepository.save(entity);
    }

    public ObjectEntity update(Long id, ObjectEntity updated) {
        ObjectEntity existing = objectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Object not found with ID: " + id));

        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setTypeId(updated.getTypeId());
        existing.setIsArchived(updated.getIsArchived());

        log.info("Updating object ID {} with new data", id);
        return objectRepository.save(existing);
    }

    public void delete(Long id) {
        if (!objectRepository.existsById(id)) {
            throw new EntityNotFoundException("Object not found with ID: " + id);
        }
        objectRepository.deleteById(id);
        log.warn("Deleted object with ID {}", id);
    }

    @Transactional(readOnly = true)
    public List<ObjectEntity> getAllActive() {
        return objectRepository.findByIsArchivedFalse();
    }

    @Transactional(readOnly = true)
    public ObjectEntity getById(Long id) {
        return objectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Object not found with ID: " + id));
    }
}
