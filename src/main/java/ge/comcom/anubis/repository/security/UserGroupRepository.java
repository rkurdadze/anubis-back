package ge.comcom.anubis.repository.security;

import ge.comcom.anubis.entity.security.UserGroup;
import ge.comcom.anubis.entity.security.UserGroupId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, UserGroupId> {

    List<UserGroup> findByIdUserId(Long userId);

    List<UserGroup> findByIdGroupId(Long groupId);

    void deleteByIdUserId(Long userId);

    void deleteByIdGroupId(Long groupId);

    boolean existsByIdUserIdAndIdGroupId(Long userId, Long groupId);
}
