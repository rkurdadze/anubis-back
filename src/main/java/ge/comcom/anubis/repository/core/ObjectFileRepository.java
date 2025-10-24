package ge.comcom.anubis.repository.core;

import ge.comcom.anubis.entity.core.ObjectFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ObjectFileRepository extends JpaRepository<ObjectFile, Long> {
    List<ObjectFile> findByVersion_Id(Long versionId);
}
