package ge.comcom.anubis.service.core;

import ge.comcom.anubis.entity.core.ObjectFileEntity;
import ge.comcom.anubis.repository.core.ObjectFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for managing stored object files.
 * Provides simple CRUD access to files by version or object.
 */
@Service
@RequiredArgsConstructor
public class ObjectFileService {

    private final ObjectFileRepository objectFileRepository;

    /**
     * Returns list of files linked to a given version ID.
     * @param versionId ID of object version
     * @return List of file entities
     */
    public List<ObjectFileEntity> getFiles(Long versionId) {
        return objectFileRepository.findByVersionId(versionId);
    }

    /**
     * Saves a new file entity.
     * @param entity file entity to persist
     * @return persisted entity
     */
    public ObjectFileEntity save(ObjectFileEntity entity) {
        return objectFileRepository.save(entity);
    }

    /**
     * Deletes file by ID.
     * @param id file ID
     */
    public void delete(Long id) {
        objectFileRepository.deleteById(id);
    }
}
