-- Move vault ownership from object to object_type
-- 1. Add vault reference to object_type
ALTER TABLE object_type
    ADD COLUMN vault_id BIGINT;

-- 2. Ensure every object_type is linked with at most one vault in current data
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM (
            SELECT object_type_id
            FROM "object"
            WHERE vault_id IS NOT NULL
            GROUP BY object_type_id
            HAVING COUNT(DISTINCT vault_id) > 1
        ) conflicting
    ) THEN
        RAISE EXCEPTION 'Cannot migrate: some object types are linked to multiple vaults';
    END IF;
END $$;

-- 3. Copy existing vault assignments from objects to their types
UPDATE object_type ot
SET vault_id = sub.vault_id
FROM (
    SELECT object_type_id, MAX(vault_id) AS vault_id
    FROM "object"
    GROUP BY object_type_id
) sub
WHERE ot.object_type_id = sub.object_type_id;

-- 4. Ensure every object type received a vault assignment
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM object_type WHERE vault_id IS NULL
    ) THEN
        RAISE EXCEPTION 'Cannot migrate: some object types have no vault assignment';
    END IF;
END $$;

-- 5. Disallow null vault references for object types
ALTER TABLE object_type
    ALTER COLUMN vault_id SET NOT NULL;

-- 6. Add foreign key constraint for the new column
ALTER TABLE object_type
    ADD CONSTRAINT object_type_vault_id_fkey
        FOREIGN KEY (vault_id) REFERENCES vault (vault_id);

-- 7. Drop the old foreign key and column from object
ALTER TABLE "object" DROP CONSTRAINT IF EXISTS object_vault_id_fkey;
ALTER TABLE "object" DROP COLUMN IF EXISTS vault_id;
