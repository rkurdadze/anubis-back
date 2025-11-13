package ge.comcom.anubis.service.storage;

import ge.comcom.anubis.entity.core.FileBinaryEntity;
import ge.comcom.anubis.entity.core.FileStorageEntity;
import ge.comcom.anubis.entity.core.ObjectFileEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Component
@Slf4j
public class DatabaseStorageStrategy implements FileStorageStrategy {

    @Override
    public void save(FileStorageEntity storage, ObjectFileEntity entity, MultipartFile file) throws IOException {
        var binary = new FileBinaryEntity();
        binary.setContent(file.getBytes());
        binary.setMimeType(file.getContentType());
        binary.setSize(file.getSize());
        entity.setBinary(binary);
        log.info("Stored file '{}' in database storage '{}'", entity.getFileName(), storage.getName());
    }

    @Override
    public byte[] load(ObjectFileEntity entity) {
        return entity.getBinary() != null ? entity.getBinary().getContent() : null;
    }

    @Override
    public void delete(ObjectFileEntity entity) {
        log.debug("Deleted inline file '{}'", entity.getFileName());
    }
}
