package ge.comcom.anubis.service.core;

import ge.comcom.anubis.entity.core.ObjectVersion;
import ge.comcom.anubis.repository.core.ObjectVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ObjectVersionService {
    private final ObjectVersionRepository objectVersionRepository;

    public List<ObjectVersion> getVersions(Long objectId) {
        return objectVersionRepository.findByObject_IdOrderByVersionNumDesc(objectId);
    }

    public Optional<ObjectVersion> getById(Long id) {
        return objectVersionRepository.findById(id);
    }

    public ObjectVersion save(ObjectVersion version) {
        return objectVersionRepository.save(version);
    }

    public void delete(Long id) {
        objectVersionRepository.deleteById(id);
    }
}
