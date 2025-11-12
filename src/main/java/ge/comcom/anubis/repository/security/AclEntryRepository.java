package ge.comcom.anubis.repository.security;

import ge.comcom.anubis.entity.security.AclEntry;
import ge.comcom.anubis.enums.GranteeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AclEntryRepository extends JpaRepository<AclEntry, Long> {

    List<AclEntry> findByAcl_Id(Long aclId);

    Optional<AclEntry> findByIdAndAcl_Id(Long id, Long aclId);

    Optional<AclEntry> findByAcl_IdAndGranteeTypeAndGranteeId(Long aclId, GranteeType granteeType, Long granteeId);

    boolean existsByAcl_IdAndGranteeTypeAndGranteeId(Long aclId, GranteeType granteeType, Long granteeId);
}
