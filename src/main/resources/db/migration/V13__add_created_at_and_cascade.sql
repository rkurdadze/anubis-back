-- ===============================================
-- 🧩 V13__add_created_at_and_cascade.sql
-- Добавление поля created_at в object_view
-- и настройка каскадного удаления для search_text_cache.
-- ===============================================

-- 1️⃣ Добавляем поле created_at в object_view
ALTER TABLE object_view
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT now();

COMMENT ON COLUMN object_view.created_at IS 'Дата создания представления (view)';

-- 2️⃣ Устанавливаем значение created_at для существующих записей
UPDATE object_view SET created_at = now() WHERE created_at IS NULL;

-- ===============================================
-- 🧩 Каскадное удаление для связей с search_text_cache
-- ===============================================

-- Когда удаляется версия (object_version), связанные записи в search_text_cache
-- должны удаляться автоматически.
ALTER TABLE search_text_cache
    DROP CONSTRAINT IF EXISTS fk_search_text_cache_version,
    ADD CONSTRAINT fk_search_text_cache_version
        FOREIGN KEY (object_version_id)
            REFERENCES object_version (version_id)
            ON DELETE CASCADE;

COMMENT ON CONSTRAINT fk_search_text_cache_version ON search_text_cache
    IS 'Каскадное удаление при удалении версии объекта.';

-- ===============================================
-- ✅ Готово
-- ===============================================
