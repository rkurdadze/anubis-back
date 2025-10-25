package ge.comcom.anubis.service.storage;

import ge.comcom.anubis.entity.core.FileStorageEntity;
import ge.comcom.anubis.entity.core.ObjectFileEntity;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface FileStorageStrategy {
    void save(FileStorageEntity storage, ObjectFileEntity entity, MultipartFile file) throws IOException;
    byte[] load(ObjectFileEntity entity) throws IOException;
    void delete(ObjectFileEntity entity) throws IOException;
}
