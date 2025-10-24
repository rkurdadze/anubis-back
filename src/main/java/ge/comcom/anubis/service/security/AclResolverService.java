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
 * Resolves effective ACL for an object version, following inheritance chain:
 * version.acl_id → object.acl_id → class.acl_id → object_type.acl_id
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AclResolverService {

    @PersistenceContext
    private EntityManager em;

    /**
     * Returns mapping: version_id → effective ACL id (resolved by inheritance).
     */
    public Map<Long, Long> resolveEffectiveAclIds(Collection<Long> versionIds) {
        if (versionIds == null || versionIds.isEmpty()) return Map.of();

        String sql = """
            SELECT 
                v.version_id,
                COALESCE(
                    v.acl_id,
                    o.acl_id,
                    c.acl_id,
                    t.acl_id
                ) AS effective_acl
            FROM object_version v
            JOIN "object" o ON o.object_id = v.object_id
            LEFT JOIN "class" c ON c.class_id = o.class_id
            LEFT JOIN object_type t ON t.object_type_id = o.object_type_id
            WHERE v.version_id = ANY(:ids)
            """;

        Query query = em.createNativeQuery(sql);
        query.setParameter("ids", versionIds);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            Number versionId = (Number) row[0];
            Number acl = (Number) row[1];
            if (acl != null)
                map.put(versionId.longValue(), acl.longValue());
        }

        log.debug("Resolved effective ACL for {} versions", map.size());
        return map;
    }
}
