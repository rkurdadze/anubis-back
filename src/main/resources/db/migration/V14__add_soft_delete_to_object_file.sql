-- ===============================================
-- 🧩 V14
-- ===============================================

-- Add "deleted" flag to object_file for soft-delete support
ALTER TABLE object_file
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- Ensure all existing rows are initialized
UPDATE object_file SET deleted = FALSE WHERE deleted IS NULL;