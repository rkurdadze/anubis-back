package ge.comcom.anubis.service.core;

import ge.comcom.anubis.entity.core.ObjectVersionEntity;
import ge.comcom.anubis.repository.core.ObjectVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ObjectVersionService {

    private final ObjectVersionRepository versionRepository;

    /**
     * Creates a new version for a given object ID.
     * If object has no versions yet — starts from version 1.
     */
    public ObjectVersionEntity createNewVersion(Long objectId, String comment) {
        Integer lastVersion = versionRepository.findLastVersionNumber(objectId);
        int newVersion = (lastVersion == null ? 1 : lastVersion + 1);

        ObjectVersionEntity entity = ObjectVersionEntity.builder()
                .objectId(objectId)
                .versionNumber(newVersion)
                .createdAt(Instant.now())
                .createdBy("system") // позже заменим на текущего пользователя
                .comment(comment != null ? comment : "Auto-created during file upload")
                .build();

        return versionRepository.save(entity);
    }

    public ObjectVersionEntity getLatestVersion(Long objectId) {
        return versionRepository.findTopByObjectIdOrderByVersionNumberDesc(objectId)
                .orElse(null);
    }

    public ObjectVersionEntity save(ObjectVersionEntity version) {
        return versionRepository.save(version);
    }

    public void delete(Long id) {
        versionRepository.deleteById(id);
    }

}
