-- ===============================================
-- 🧩 V17
-- ===============================================

-- Introduce self-referencing hierarchy for metadata classes
ALTER TABLE "class"
    ADD COLUMN parent_class_id BIGINT NULL;

ALTER TABLE "class"
    ADD CONSTRAINT class_parent_class_id_fkey
        FOREIGN KEY (parent_class_id)
        REFERENCES "class"(class_id)
        ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS class_parent_class_id_idx
    ON "class"(parent_class_id);
