package ge.comcom.anubis.repository.security;

import ge.comcom.anubis.entity.security.Acl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Access Control Lists (ACL).
 * Provides CRUD operations and helper methods for name-based lookup.
 */
@Repository
public interface AclRepository extends JpaRepository<Acl, Long> {

    /**
     * Checks if ACL with the given name already exists (case-insensitive).
     */
    boolean existsByNameIgnoreCase(String name);
}
