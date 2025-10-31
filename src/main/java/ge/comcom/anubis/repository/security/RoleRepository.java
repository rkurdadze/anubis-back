package ge.comcom.anubis.repository.security;

import ge.comcom.anubis.entity.security.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    boolean existsByNameIgnoreCase(String name);

    Optional<Role> findByNameIgnoreCase(String name);

    List<Role> findByIdIn(Collection<Long> ids);
}
