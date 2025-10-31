-- =========================================================
-- Flyway Migration: V11__extend_security_model.sql
-- Purpose:
--   * Introduce user status enum (ACTIVE/INACTIVE/LOCKED).
--   * Add security_role catalog with user/group role assignments.
--   * Allow ACL entries to target roles in addition to users and groups.
--   * Seed a couple of default roles mirroring common M-Files roles.
-- =========================================================

BEGIN;

-- 1️⃣ Ensure enum type for user status exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_type WHERE typname = 'user_status_enum'
    ) THEN
        CREATE TYPE user_status_enum AS ENUM ('ACTIVE', 'INACTIVE', 'LOCKED');
    END IF;
END$$;

-- 2️⃣ Add status column to user table with default ACTIVE
ALTER TABLE "user"
    ADD COLUMN IF NOT EXISTS status user_status_enum NOT NULL DEFAULT 'ACTIVE';

COMMENT ON COLUMN "user".status IS
    'Current login status. ACTIVE users can authenticate, INACTIVE/LOCKED are disabled.';

-- Ensure existing rows have a status value (for older databases)
UPDATE "user" SET status = 'ACTIVE' WHERE status IS NULL;

-- 3️⃣ Create security_role catalog (if not already present)
CREATE TABLE IF NOT EXISTS security_role
(
    role_id     SERIAL PRIMARY KEY,
    name        TEXT    NOT NULL UNIQUE,
    description TEXT,
    is_system   BOOLEAN NOT NULL DEFAULT FALSE,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE
);

COMMENT ON TABLE security_role IS 'Reusable security roles akin to M-Files Named Roles.';
COMMENT ON COLUMN security_role.name IS 'Unique role name (e.g. "Full Control").';
COMMENT ON COLUMN security_role.is_system IS 'TRUE for built-in roles that cannot be removed.';
COMMENT ON COLUMN security_role.is_active IS 'FALSE to temporarily disable a role.';

-- 4️⃣ Link users to roles
CREATE TABLE IF NOT EXISTS user_role
(
    user_id INT NOT NULL REFERENCES "user"(user_id) ON DELETE CASCADE,
    role_id INT NOT NULL REFERENCES security_role(role_id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX IF NOT EXISTS idx_user_role_role_id ON user_role(role_id);

COMMENT ON TABLE user_role IS 'Assignments of users to security roles.';

-- 5️⃣ Link groups to roles
CREATE TABLE IF NOT EXISTS group_role
(
    group_id INT NOT NULL REFERENCES "group"(group_id) ON DELETE CASCADE,
    role_id  INT NOT NULL REFERENCES security_role(role_id) ON DELETE CASCADE,
    PRIMARY KEY (group_id, role_id)
);

CREATE INDEX IF NOT EXISTS idx_group_role_role_id ON group_role(role_id);

COMMENT ON TABLE group_role IS 'Assignments of groups to security roles.';

-- 6️⃣ Allow ACL entries to reference roles directly
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_enum
        WHERE enumlabel = 'ROLE'
          AND enumtypid = 'grantee_type_enum'::regtype
    ) THEN
        ALTER TYPE grantee_type_enum ADD VALUE 'ROLE';
    END IF;
END$$;

-- 7️⃣ Seed default roles inspired by M-Files
INSERT INTO security_role (name, description, is_system)
VALUES ('Full Control', 'Equivalent to Full Control access rights in M-Files.', TRUE),
       ('Editor', 'Can read and modify content but cannot delete or change ACL.', TRUE),
       ('Viewer', 'Read-only access.', TRUE)
ON CONFLICT (name) DO NOTHING;

COMMIT;

-- =========================================================
-- ✅ Migration complete
-- =========================================================
