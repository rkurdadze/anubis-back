package ge.comcom.anubis.service.storage;

import ge.comcom.anubis.entity.core.FileStorageEntity;
import ge.comcom.anubis.enums.StorageKindEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StorageStrategyRegistry {

    private final DatabaseStorageStrategy db;
    private final DiskStorageStrategy fs;
    private final S3StorageStrategy s3;

    public FileStorageStrategy resolve(FileStorageEntity storage) {
        if (storage == null || storage.getKind() == null)
            return db;

        return switch (storage.getKind()) {
            case DB -> db;
            case FS -> fs;
            case S3 -> s3;
        };
    }
}
