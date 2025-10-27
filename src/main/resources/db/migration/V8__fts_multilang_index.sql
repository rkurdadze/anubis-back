-- ==========================================================
-- GIN ინდექსი multilang კონფიგურაციისთვის
-- ==========================================================

-- ძველი ინდექსის წაშლა, თუ არსებობს
DROP INDEX IF EXISTS idx_search_text_cache_fts;

-- ახალი GIN ინდექსის შექმნა tsvector ველზე
CREATE INDEX idx_search_text_cache_fts
    ON search_text_cache
        USING gin (extracted_text_vector)
    WITH (fastupdate = on);

-- ✅ ტესტი:
-- ამ ბრძანებამ უნდა დააბრუნოს შედეგი სწრაფად (< 1 ms):
-- EXPLAIN ANALYZE
-- SELECT object_version_id
-- FROM search_text_cache
-- WHERE extracted_text_vector @@ websearch_to_tsquery('multilang', 'დოკუმენტი | document | документ');