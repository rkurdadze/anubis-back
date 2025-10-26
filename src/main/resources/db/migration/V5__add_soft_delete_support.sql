-- ===============================================
-- V5__add_soft_delete_support.sql
-- Purpose: enable soft delete for meta tables
-- ===============================================

BEGIN;

-- 1️⃣ property_def
ALTER TABLE property_def
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;

COMMENT ON COLUMN property_def.is_active IS
    'Soft delete flag for property definitions.';

-- 2️⃣ value_list
ALTER TABLE value_list
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;

COMMENT ON COLUMN value_list.is_active IS
    'Soft delete flag for value lists.';

-- 3️⃣ value_list_item (уже есть is_active, но на всякий случай обновляем default)
ALTER TABLE value_list_item
    ALTER COLUMN is_active SET DEFAULT TRUE;

-- 4️⃣ class_property — возможность отключить без удаления
ALTER TABLE class_property
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;

COMMENT ON COLUMN class_property.is_active IS
    'Soft delete flag for class-property bindings.';

COMMIT;
