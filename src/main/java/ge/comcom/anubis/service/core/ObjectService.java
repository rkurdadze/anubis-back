package ge.comcom.anubis.service.core;

import ge.comcom.anubis.entity.core.ObjectEntity;
import ge.comcom.anubis.repository.core.ObjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ObjectService {
    private final ObjectRepository objectRepository;

    public List<ObjectEntity> findAll() {
        return objectRepository.findAll();
    }

    public Optional<ObjectEntity> findById(Long id) {
        return objectRepository.findById(id);
    }

    public ObjectEntity save(ObjectEntity entity) {
        return objectRepository.save(entity);
    }

    public void delete(Long id) {
        objectRepository.deleteById(id);
    }
}
