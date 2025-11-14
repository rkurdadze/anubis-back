-- ===============================================
-- 🧩 V15
-- ===============================================

-- ============================================
-- 1. CREATE TABLE file_binary (PHYSICAL FILE)
-- ============================================
CREATE TABLE IF NOT EXISTS file_binary (
                                           binary_id      BIGSERIAL PRIMARY KEY,
                                           sha256         VARCHAR(128),
                                           inline         BOOLEAN NOT NULL DEFAULT TRUE,
                                           content        BYTEA,
                                           external_path  TEXT,
                                           size           BIGINT,
                                           mime_type      VARCHAR(255),
                                           created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- =====================================================
-- 2. ALTER TABLE object_file → LINK TO BINARY TABLE
-- =====================================================

-- Add binary_id if missing
ALTER TABLE object_file
    ADD COLUMN IF NOT EXISTS binary_id BIGINT;

-- Add FK if missing
DO $$ BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_object_file_binary'
    ) THEN
        ALTER TABLE object_file
            ADD CONSTRAINT fk_object_file_binary
                FOREIGN KEY (binary_id)
                    REFERENCES file_binary(binary_id);
    END IF;
END $$;

-- ============================
-- 3. DROP OLD INLINE COLUMNS
-- ============================

ALTER TABLE object_file DROP COLUMN IF EXISTS file_data;
ALTER TABLE object_file DROP COLUMN IF EXISTS external_file_path;
ALTER TABLE object_file DROP COLUMN IF EXISTS inline;
ALTER TABLE object_file DROP COLUMN IF EXISTS content_type;
ALTER TABLE object_file DROP COLUMN IF EXISTS file_size;
ALTER TABLE object_file DROP COLUMN IF EXISTS content_sha256;

-- ======================================
-- 4. DO NOT ADD file_name, mime_type, deleted
--    → they already exist in your schema
-- ======================================