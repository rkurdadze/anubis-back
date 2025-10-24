package ge.comcom.anubis.service.view;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ge.comcom.anubis.dto.view.ObjectViewFilter;
import ge.comcom.anubis.entity.core.ObjectVersionEntity;
import ge.comcom.anubis.entity.view.ObjectViewEntity;
import ge.comcom.anubis.repository.core.ObjectVersionRepository;
import ge.comcom.anubis.repository.meta.PropertyValueRepository;
import ge.comcom.anubis.repository.view.ObjectViewRepository;
import ge.comcom.anubis.service.FullTextSearchService;
import ge.comcom.anubis.service.security.AclResolverService;
import ge.comcom.anubis.service.security.AclService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Executes saved view filter_json and returns matching ObjectVersionEntities.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ObjectViewExecutionService {

    private final ObjectViewRepository viewRepository;
    private final ObjectVersionRepository versionRepository;
    private final PropertyValueRepository propertyValueRepository;
    private final ObjectMapper objectMapper;
    // import ge.comcom.anubis.service.search.FullTextSearchService;
    private final FullTextSearchService fullTextSearchService;

    private final AclResolverService aclResolverService;

    private final AclService aclService;


    @PersistenceContext
    private EntityManager em;

    /**
     * Executes the saved view with provided ID and returns matching object versions.
     *
     * @param viewId ID of object_view
     * @return list of ObjectVersionEntity matching filters
     */
    public List<ObjectVersionEntity> execute(Long viewId, Long userId) {
        ObjectViewEntity view = viewRepository.findById(viewId)
                .orElseThrow(() -> new IllegalArgumentException("View not found: " + viewId));

        List<ObjectViewFilter> filters = parseFilters(view.getFilterJson());
        String fullText = extractFullText(filters);

        Set<Long> readableAcls = aclService.getReadableAclIds(userId);
        if (readableAcls.isEmpty()) {
            log.info("User {} has no readable ACLs -> empty result", userId);
            return List.of();
        }

        // --- Full-text search
        Set<Long> ftsIds = null;
        if (fullText != null && !fullText.isBlank()) {
            ftsIds = fullTextSearchService.findMatchingVersionIds(fullText);
            if (ftsIds.isEmpty()) {
                log.info("FTS returned no results for '{}'", fullText);
                return List.of();
            }
        }

        // --- Build base query (property filters)
        StringBuilder sql = new StringBuilder("""
            SELECT DISTINCT v.version_id
            FROM object_version v
            JOIN "object" o ON o.object_id = v.object_id
            LEFT JOIN property_value pv ON pv.object_version_id = v.version_id
            WHERE 1=1
            """);

        Map<String, Object> params = new HashMap<>();

        if (ftsIds != null && !ftsIds.isEmpty()) {
            sql.append("AND v.version_id = ANY(:ftsIds) ");
            params.put("ftsIds", ftsIds);
        }

        int idx = 0;
        for (ObjectViewFilter f : filters) {
            if (f.getPropertyDefId() == null || f.getPropertyDefId() == 0) continue;
            idx++;
            String alias = "p" + idx;
            sql.append("AND pv.property_def_id = :").append(alias).append("_def ");
            sql.append("AND pv.value_text = :").append(alias).append("_val ");
            params.put(alias + "_def", f.getPropertyDefId());
            params.put(alias + "_val", f.getValue());
        }

        Query query = em.createNativeQuery(sql.toString());
        params.forEach(query::setParameter);

        @SuppressWarnings("unchecked")
        List<Number> versionIdsRaw = query.getResultList();
        if (versionIdsRaw.isEmpty()) {
            log.debug("No matches before ACL filtering");
            return List.of();
        }

        Set<Long> versionIds = new HashSet<>();
        for (Number n : versionIdsRaw) versionIds.add(n.longValue());

        // --- Resolve effective ACLs
        Map<Long, Long> effectiveAcls = aclResolverService.resolveEffectiveAclIds(versionIds);

        // --- Apply ACL restriction
        Set<Long> allowedVersions = new HashSet<>();
        for (Map.Entry<Long, Long> e : effectiveAcls.entrySet()) {
            if (readableAcls.contains(e.getValue())) {
                allowedVersions.add(e.getKey());
            }
        }

        if (allowedVersions.isEmpty()) {
            log.info("User {} has no readable versions in result set", userId);
            return List.of();
        }

        // --- Fetch entities
        String finalSql = """
            SELECT * FROM object_version
            WHERE version_id = ANY(:ids)
            """;
        Query q2 = em.createNativeQuery(finalSql, ObjectVersionEntity.class);
        q2.setParameter("ids", allowedVersions);

        @SuppressWarnings("unchecked")
        List<ObjectVersionEntity> result = q2.getResultList();

        log.info("Executed view {} for user {} -> {} versions", viewId, userId, result.size());
        return result;
    }

    private String extractFullText(List<ObjectViewFilter> filters) {
        return filters.stream()
                .filter(f -> f.getPropertyDefId() != null && f.getPropertyDefId() == 0L) // convention: propertyDefId=0 означает FTS
                .map(ObjectViewFilter::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst()
                .orElse(null);
    }


    private List<ObjectViewFilter> parseFilters(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Invalid filter_json for view: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
