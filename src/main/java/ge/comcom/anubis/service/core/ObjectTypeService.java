package ge.comcom.anubis.service.core;

import ge.comcom.anubis.entity.core.ObjectType;
import ge.comcom.anubis.repository.core.ObjectTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ObjectTypeService {

    private final ObjectTypeRepository objectTypeRepository;

    public List<ObjectType> findAll() {
        return objectTypeRepository.findAll();
    }

    public ObjectType findById(Long id) {
        return objectTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ObjectType not found: " + id));
    }

    public ObjectType create(ObjectType objectType) {
        return objectTypeRepository.save(objectType);
    }

    public ObjectType update(Long id, ObjectType updated) {
        ObjectType existing = findById(id);
        existing.setName(updated.getName());
        return objectTypeRepository.save(existing);
    }

    public void delete(Long id) {
        objectTypeRepository.deleteById(id);
    }
}
