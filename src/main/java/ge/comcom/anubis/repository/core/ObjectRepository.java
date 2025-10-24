package ge.comcom.anubis.repository.core;

import ge.comcom.anubis.entity.core.ObjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObjectRepository extends JpaRepository<ObjectEntity, Long> {
}
