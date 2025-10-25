package ge.comcom.anubis.service.storage;

import ge.comcom.anubis.entity.core.FileStorageEntity;
import ge.comcom.anubis.entity.core.ObjectFileEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

@Component
@Slf4j
public class DiskStorageStrategy implements FileStorageStrategy {

    @Override
    public void save(FileStorageEntity storage, ObjectFileEntity entity, MultipartFile file) throws IOException {
        Path base = Paths.get(storage.getBasePath());
        Files.createDirectories(base);

        Path path = base.resolve(file.getOriginalFilename());
        Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

        entity.setInline(false);
        entity.setExternalFilePath(path.toString());
        entity.setContent(null);

        log.info("Stored file '{}' on disk at {}", entity.getFileName(), path);
    }

    @Override
    public byte[] load(ObjectFileEntity entity) throws IOException {
        return Files.readAllBytes(Paths.get(entity.getExternalFilePath()));
    }

    @Override
    public void delete(ObjectFileEntity entity) throws IOException {
        Files.deleteIfExists(Paths.get(entity.getExternalFilePath()));
    }
}
