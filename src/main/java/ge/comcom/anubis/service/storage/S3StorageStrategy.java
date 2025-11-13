package ge.comcom.anubis.service.storage;

import ge.comcom.anubis.entity.core.FileBinaryEntity;
import ge.comcom.anubis.entity.core.FileStorageEntity;
import ge.comcom.anubis.entity.core.ObjectFileEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Component
@Slf4j
public class S3StorageStrategy implements FileStorageStrategy {

    @Override
    public void save(FileStorageEntity storage, ObjectFileEntity entity, MultipartFile file) throws IOException {
        String key = UUID.randomUUID() + "_" + file.getOriginalFilename();
        // Здесь можно вызвать MinIO / AWS SDK клиент для загрузки
        var binary = new FileBinaryEntity();
        binary.setInline(false);
        binary.setExternalPath(key);
        binary.setContent(null);
        binary.setMimeType(file.getContentType());
        binary.setSize(file.getSize());
        entity.setBinary(binary);
        log.info("Stored file '{}' in S3 bucket '{}' with key '{}'", file.getOriginalFilename(), storage.getBucket(), key);
    }

    @Override
    public byte[] load(ObjectFileEntity entity) {
        throw new UnsupportedOperationException("S3 download not implemented yet");
    }

    @Override
    public void delete(ObjectFileEntity entity) {
        log.info("Deleted file from S3: {}",
                 entity.getBinary() != null ? entity.getBinary().getExternalPath() : null);
    }
}
