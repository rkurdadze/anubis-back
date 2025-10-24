package ge.comcom.anubis.service.core;

import ge.comcom.anubis.entity.core.ObjectFile;
import ge.comcom.anubis.repository.core.ObjectFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ObjectFileService {
    private final ObjectFileRepository objectFileRepository;

    public List<ObjectFile> getFiles(Long versionId) {
        return objectFileRepository.findByVersion_Id(versionId);
    }

    public ObjectFile save(ObjectFile file) {
        return objectFileRepository.save(file);
    }

    public void delete(Long id) {
        objectFileRepository.deleteById(id);
    }
}
