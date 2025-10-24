package ge.comcom.anubis.repository.core;

import ge.comcom.anubis.entity.core.LinkRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LinkRoleRepository extends JpaRepository<LinkRole, Long> {

    Optional<LinkRole> findByNameIgnoreCase(String name);
}
