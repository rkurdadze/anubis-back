-- =========================================================
-- Flyway Migration: V2__add_link_role.sql
-- Purpose:
-- Introduce centralized link_role table for relationship definitions.
-- Migrate existing text-based roles in object_link to link_role references.
-- Add default system roles similar to M-Files relationship definitions.
-- =========================================================

BEGIN;

-- 1️⃣ Create new table link_role
CREATE TABLE object_link_role
(
    role_id            SERIAL PRIMARY KEY,
    name               TEXT NOT NULL UNIQUE,
    name_i18n          JSONB               DEFAULT '{}'::jsonb,
    description        TEXT,
    direction          link_direction_enum DEFAULT 'UNI',
    src_object_type_id INT REFERENCES object_type (object_type_id),
    dst_object_type_id INT REFERENCES object_type (object_type_id),
    is_active          BOOLEAN             DEFAULT TRUE
);

COMMENT ON TABLE object_link_role IS
    'Defines available relationship roles between object types (similar to M-Files Relationship Definitions).';
COMMENT ON COLUMN object_link_role.role_id IS 'Primary key. Example: 10.';
COMMENT ON COLUMN object_link_role.name IS 'Role name. Example: "Customer".';
COMMENT ON COLUMN object_link_role.name_i18n IS 'Localized names, e.g. {"en":"Customer","ru":"Клиент"}.';
COMMENT ON COLUMN object_link_role.description IS 'Optional textual description for UI and documentation.';
COMMENT ON COLUMN object_link_role.direction IS 'Default direction for this role (UNI or BI).';
COMMENT ON COLUMN object_link_role.src_object_type_id IS 'Optional restriction: allowed source object type.';
COMMENT ON COLUMN object_link_role.dst_object_type_id IS 'Optional restriction: allowed destination object type.';
COMMENT ON COLUMN object_link_role.is_active IS 'TRUE if the role is available for use.';

-- 2️⃣ Add new column role_id to object_link (nullable for migration)
ALTER TABLE object_link
    ADD COLUMN role_id INT REFERENCES object_link_role (role_id);

-- 3️⃣ Copy all distinct text roles from object_link into link_role
INSERT INTO object_link_role (name)
SELECT DISTINCT role
FROM object_link
WHERE role IS NOT NULL
ORDER BY role;

-- 4️⃣ Add default system roles (if not already present)
INSERT INTO object_link_role (name, name_i18n, description, direction)
VALUES ('Customer', '{
  "en": "Customer",
  "ru": "Клиент"
}', 'Link from Document or Contract to Customer object', 'BI'),
       ('Supplier', '{
         "en": "Supplier",
         "ru": "Поставщик"
       }', 'Link from Document or Contract to Supplier object', 'BI'),
       ('Project', '{
         "en": "Project",
         "ru": "Проект"
       }', 'Link from Document to Project container', 'UNI'),
       ('Attachment', '{
         "en": "Attachment",
         "ru": "Вложение"
       }', 'Attached files or related documents', 'UNI')
ON CONFLICT (name) DO NOTHING;

-- 5️⃣ Update object_link to reference the corresponding role_id
UPDATE object_link ol
SET role_id = lr.role_id
FROM object_link_role lr
WHERE ol.role = lr.name;

-- 6️⃣ Make role_id mandatory (now that data is filled)
ALTER TABLE object_link
    ALTER COLUMN role_id SET NOT NULL;

-- 7️⃣ Drop old text-based role column
ALTER TABLE object_link
    DROP COLUMN role;

-- 8️⃣ Add new unique constraint based on role_id instead of text
ALTER TABLE object_link
    ADD CONSTRAINT uq_object_link_src_dst_roleid UNIQUE (src_object_id, dst_object_id, role_id);

-- 9️⃣ Add helpful indexes for performance
CREATE INDEX idx_object_link_roleid ON object_link (role_id);
CREATE INDEX idx_link_role_name ON object_link_role (name);

-- 🔟 Optional audit log message
DO
$$
    BEGIN
        RAISE NOTICE '✅ Migration V2__add_link_role completed. % roles now exist in link_role table.',
                (SELECT COUNT(*) FROM object_link_role);
    END
$$;

COMMIT;

-- =========================================================
-- ✅ Migration complete
-- =========================================================
-- After migration:
--  - Roles are centralized in link_role table.
--  - object_link now references object_link_role by FK.
--  - Existing data migrated automatically.
--  - Default roles (Customer, Supplier, Project, Attachment) are created.
-- =========================================================
