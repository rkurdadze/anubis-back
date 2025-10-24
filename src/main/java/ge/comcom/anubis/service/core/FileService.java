package ge.comcom.anubis.service.core;

import ge.comcom.anubis.dto.core.ObjectFileDto;
import ge.comcom.anubis.entity.core.ObjectFileEntity;
import ge.comcom.anubis.repository.core.ObjectFileRepository;
import ge.comcom.anubis.repository.core.ObjectVersionRepository;
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
        // auto-create new version
        var version = versionService.createNewVersion(objectId, "Auto-version from upload");

        ObjectFileEntity entity = new ObjectFileEntity();
        entity.setObjectId(objectId);
        entity.setVersionId(version.getId());
        entity.setFilename(file.getOriginalFilename());
        entity.setMimeType(file.getContentType());
        entity.setSize(file.getSize());
        entity.setUploadedAt(Instant.now());
        entity.setData(file.getBytes());

        ObjectFileEntity saved = fileRepository.save(entity);
        return toDto(saved);
    }

    private ObjectFileDto toDto(ObjectFileEntity entity) {
        return ObjectFileDto.builder()
                .id(entity.getId())
                .objectId(entity.getObjectId())
                .versionId(entity.getVersionId())
                .filename(entity.getFilename())
                .mimeType(entity.getMimeType())
                .size(entity.getSize())
                .uploadedAt(entity.getUploadedAt().toString())
                .build();
    }
}
