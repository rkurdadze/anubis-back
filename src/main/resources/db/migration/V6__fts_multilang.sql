-- ===============================
-- V6__fts_multilang.sql
-- Миграция для многоязычного FTS
-- ===============================

-- 1. Создаём поле с исходным текстом (если его нет)
ALTER TABLE search_text_cache
    ADD COLUMN IF NOT EXISTS extracted_text_raw text;

-- 2. Переносим данные из старого поля, если оно существует
DO $$
    BEGIN
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'search_text_cache' AND column_name = 'extracted_text'
        ) THEN
            EXECUTE $sql$
            UPDATE search_text_cache
            SET extracted_text_raw = extracted_text::text
            WHERE extracted_text_raw IS NULL
              AND extracted_text IS NOT NULL
        $sql$;
        END IF;
    END $$;

-- 3. Удаляем старое поле extracted_text, если оно есть
ALTER TABLE search_text_cache DROP COLUMN IF EXISTS extracted_text;

-- 4. Удаляем старое векторное поле
ALTER TABLE search_text_cache DROP COLUMN IF EXISTS extracted_text_vector;

-- 5. Добавляем новое вычисляемое tsvector-поле (ссылается на extracted_text_raw)
ALTER TABLE search_text_cache
    ADD COLUMN extracted_text_vector tsvector
        GENERATED ALWAYS AS (to_tsvector('simple', coalesce(extracted_text_raw, ''))) STORED;

-- 6. Пересоздаём индекс
DROP INDEX IF EXISTS idx_search_text_cache_fts;
CREATE INDEX idx_search_text_cache_fts
    ON search_text_cache USING gin (extracted_text_vector);

-- 7. Обновляем timestamp
ALTER TABLE search_text_cache
    ADD COLUMN IF NOT EXISTS updated_at timestamp DEFAULT now();