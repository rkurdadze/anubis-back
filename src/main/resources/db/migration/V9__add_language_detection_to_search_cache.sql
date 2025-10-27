-- ========================================
-- V9__add_language_detection_to_search_cache.sql
-- Добавляет колонки для информации о языке.
-- ========================================

ALTER TABLE search_text_cache
    ADD COLUMN IF NOT EXISTS detected_language   varchar(32),
    ADD COLUMN IF NOT EXISTS language_confidence double precision;

COMMENT ON COLUMN search_text_cache.detected_language IS 'Language code detected from extracted text (BCP-47).';
COMMENT ON COLUMN search_text_cache.language_confidence IS 'Confidence score reported by language detector (0..1).';