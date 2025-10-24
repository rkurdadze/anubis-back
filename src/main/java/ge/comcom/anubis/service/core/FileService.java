package ge.comcom.anubis.service.core;

import ge.comcom.anubis.dto.ObjectFileDto;
import ge.comcom.anubis.entity.core.ObjectFileEntity;
import ge.comcom.anubis.repository.core.ObjectFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileService {

    private final ObjectFileRepository fileRepository;
    private final ObjectVersionService versionService;

    public List<ObjectFileDto> getFilesByObject(Long objectId) {
        return fileRepository.findByObjectId(objectId).stream()
                .map(this::toDto)
                .toList();
    }

    public ObjectFileEntity getFile(Long id) {
        return fileRepository.findById(id).orElse(null);
    }

    public ObjectFileDto saveFile(Long objectId, MultipartFile file) throws IOException {
        // Auto-create new version for this object
        var version = versionService.createNewVersion(objectId, "Auto-version from upload");

        // Create a lightweight reference to the version
        ObjectFileEntity entity = new ObjectFileEntity();
        entity.setVersion(version);
        entity.setFileName(file.getOriginalFilename());
        entity.setMimeType(file.getContentType());
        entity.setFileSize(file.getSize());
        entity.setUploadedBy("system"); // TODO: replace with authenticated user
        entity.setUploadedAt(Instant.now());
        entity.setContent(file.getBytes());

        ObjectFileEntity saved = fileRepository.save(entity);
        return toDto(saved);
    }


    private ObjectFileDto toDto(ObjectFileEntity entity) {
        return ObjectFileDto.builder()
                .id(entity.getId())
                .objectId(entity.getVersion() != null && entity.getVersion().getObject() != null
                        ? entity.getVersion().getObject().getId() : null)
                .versionId(entity.getVersion() != null ? entity.getVersion().getId() : null)
                .filename(entity.getFileName())
                .mimeType(entity.getMimeType())
                .size(entity.getFileSize())
                .uploadedAt(entity.getUploadedAt().toString())
                .build();
    }

}
