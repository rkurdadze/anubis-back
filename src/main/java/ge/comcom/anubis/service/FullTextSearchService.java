package ge.comcom.anubis.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides full-text search over extracted_text (tsvector) in search_text_cache.
 * Uses PostgreSQL websearch_to_tsquery for natural syntax.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FullTextSearchService {

    @PersistenceContext
    private EntityManager em;

    /**
     * Finds all object_version_id that match given text.
     * @param queryText user search phrase
     * @return Set of version ids matching the query
     */
    public Set<Long> findMatchingVersionIds(String queryText) {
        if (queryText == null || queryText.isBlank()) return Set.of();

        String sql = """
            SELECT object_version_id
            FROM search_text_cache
            WHERE extracted_text @@ websearch_to_tsquery('english', :q)
            """;

        Query query = em.createNativeQuery(sql);
        query.setParameter("q", queryText);

        @SuppressWarnings("unchecked")
        List<Number> ids = query.getResultList();
        Set<Long> result = new HashSet<>();
        for (Number n : ids) result.add(n.longValue());

        log.debug("FTS found {} versions for query '{}'", result.size(), queryText);
        return result;
    }
}
