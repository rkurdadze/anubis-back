package ge.comcom.anubis.service.security;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Resolves all ACLs that a given user can read.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AclService {

    @PersistenceContext
    private EntityManager em;

    /**
     * Returns all ACL IDs where user has read access (directly or via group).
     */
    public Set<Long> getReadableAclIds(Long userId) {
        String sql = """
            SELECT DISTINCT a.acl_id
            FROM acl_entry e
            JOIN acl a ON a.acl_id = e.acl_id
            WHERE (
                (e.grantee_type = 'USER' AND e.grantee_id = :uid)
                OR
                (e.grantee_type = 'GROUP' AND e.grantee_id IN (
                    SELECT group_id FROM user_group WHERE user_id = :uid
                ))
            )
            AND e.can_read = true
            """;

        Query query = em.createNativeQuery(sql);
        query.setParameter("uid", userId);

        @SuppressWarnings("unchecked")
        List<Number> ids = query.getResultList();
        Set<Long> result = new HashSet<>();
        for (Number n : ids) result.add(n.longValue());

        log.debug("User {} has read access to {} ACLs", userId, result.size());
        return result;
    }
}
