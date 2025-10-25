BEGIN;

-- =========================================================
-- EXTEND FILE STORAGE CONFIGURATION
-- =========================================================

-- 1️⃣ Добавляем описание "vault"-уровня, если нужно группировать объекты
CREATE TABLE IF NOT EXISTS vault (
                                     vault_id SERIAL PRIMARY KEY,
                                     code TEXT NOT NULL UNIQUE,
                                     name TEXT NOT NULL,
                                     description TEXT,
                                     default_storage_id INT REFERENCES file_storage(storage_id) ON DELETE SET NULL,
                                     is_active BOOLEAN DEFAULT TRUE
);
COMMENT ON TABLE vault IS
    'Logical repository grouping (vault). Each vault can have its own default storage.';
COMMENT ON COLUMN vault.vault_id IS 'PK. Example: 1.';
COMMENT ON COLUMN vault.code IS 'Unique vault code. Example: "finance".';
COMMENT ON COLUMN vault.name IS 'Display name. Example: "Finance Vault".';
COMMENT ON COLUMN vault.description IS 'Optional description.';
COMMENT ON COLUMN vault.default_storage_id IS 'Default file_storage used for this vault. Example: 1.';

-- Добавляем vault_id к объектам
ALTER TABLE "object"
    ADD COLUMN IF NOT EXISTS vault_id INT REFERENCES vault(vault_id) ON DELETE SET NULL;

COMMENT ON COLUMN "object".vault_id IS
    'FK to vault (repository). Defines which vault this object belongs to.';

-- 2️⃣ Добавляем расширенные поля в file_storage
ALTER TABLE file_storage
    ADD COLUMN IF NOT EXISTS is_default BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;

COMMENT ON COLUMN file_storage.is_default IS 'If TRUE, used as fallback for all new objects.';
COMMENT ON COLUMN file_storage.description IS 'Human-readable description of the storage.';
COMMENT ON COLUMN file_storage.is_active IS 'Active flag for logical disabling.';

-- 3️⃣ Добавляем индекс для ускоренного поиска
CREATE INDEX IF NOT EXISTS idx_file_storage_kind ON file_storage(kind);

COMMIT;
