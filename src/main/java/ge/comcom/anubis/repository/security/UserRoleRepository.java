package ge.comcom.anubis.repository.security;

import ge.comcom.anubis.entity.security.UserRole;
import ge.comcom.anubis.entity.security.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findByIdUserId(Long userId);

    void deleteByIdUserId(Long userId);

    boolean existsByIdUserIdAndIdRoleId(Long userId, Long roleId);

    boolean existsByIdRoleId(Long roleId);
}
