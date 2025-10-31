package ge.comcom.anubis.repository.security;

import ge.comcom.anubis.entity.security.GroupRole;
import ge.comcom.anubis.entity.security.GroupRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRoleRepository extends JpaRepository<GroupRole, GroupRoleId> {

    List<GroupRole> findByIdGroupId(Long groupId);

    void deleteByIdGroupId(Long groupId);

    boolean existsByIdRoleId(Long roleId);
}
