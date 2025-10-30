package ge.comcom.anubis.repository.security;

import ge.comcom.anubis.entity.security.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    boolean existsByNameIgnoreCase(String name);

    Optional<Group> findByNameIgnoreCase(String name);
}
