package ge.comcom.anubis.repository.core;

import ge.comcom.anubis.entity.core.SearchTextCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SearchTextCacheRepository extends JpaRepository<SearchTextCache, Long> {

    @Query(value = """
        SELECT object_version_id
        FROM search_text_cache
        WHERE extracted_text_vector @@ websearch_to_tsquery('multilang', :query)
        ORDER BY updated_at DESC
        LIMIT 100
        """, nativeQuery = true)
    List<Long> searchByText(@Param("query") String query);
}