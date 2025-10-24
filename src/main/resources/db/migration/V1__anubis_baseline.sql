-- Flyway: V1__anubis_baseline.sql
-- Creates full M-Files-like repository schema with exhaustive comments & examples.

BEGIN;

-- =========================================================
-- ENUM TYPES
-- =========================================================
DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'data_type_enum') THEN
            CREATE TYPE data_type_enum AS ENUM ('TEXT','NUMBER','DATE','BOOLEAN','LOOKUP','VALUELIST');
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'grantee_type_enum') THEN
            CREATE TYPE grantee_type_enum AS ENUM ('USER','GROUP');
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'link_direction_enum') THEN
            CREATE TYPE link_direction_enum AS ENUM ('UNI','BI');
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'storage_kind_enum') THEN
            CREATE TYPE storage_kind_enum AS ENUM ('DB','FS','S3');
        END IF;
    END$$;

COMMENT ON TYPE data_type_enum IS
    'Allowed property value types:
     - TEXT: e.g., "John Smith"
     - NUMBER: e.g., 1200.50
     - DATE: e.g., 2025-10-20 12:30
     - BOOLEAN: e.g., TRUE
     - LOOKUP: refers to another object (object_id), e.g., 53 (Customer)
     - VALUELIST: refers to value_list_item, e.g., "Approved".';

COMMENT ON TYPE grantee_type_enum IS
    'ACL grantee kind:
     - USER: single user
     - GROUP: user group.
    Example: "GROUP".';

COMMENT ON TYPE link_direction_enum IS
    'Relationship direction:
     - UNI: unidirectional, e.g., Contract -> Customer
     - BI: bidirectional.
    Example: "UNI".';

COMMENT ON TYPE storage_kind_enum IS
    'Physical file storage type:
     - DB: stored in BYTEA
     - FS: filesystem (base_path)
     - S3: object store bucket.
    Example: "S3".';

-- =========================================================
-- USERS / GROUPS
-- =========================================================
CREATE TABLE "user" (
                        user_id SERIAL PRIMARY KEY,
                        username TEXT NOT NULL UNIQUE,
                        full_name TEXT,
                        password_hash TEXT
);
COMMENT ON TABLE "user" IS
    'System users. Example: {user_id:1, username:"admin", full_name:"System Administrator"}.';
COMMENT ON COLUMN "user".user_id IS 'Primary key. Example: 1.';
COMMENT ON COLUMN "user".username IS 'Unique login. Example: "jsmith".';
COMMENT ON COLUMN "user".full_name IS 'UI display name. Example: "John Smith".';
COMMENT ON COLUMN "user".password_hash IS 'Optional password hash (SSO recommended). Example: "$2b$12$...".';

CREATE TABLE "group" (
                         group_id SERIAL PRIMARY KEY,
                         name TEXT NOT NULL UNIQUE
);
COMMENT ON TABLE "group" IS
    'User groups for permissions. Example: {group_id:10, name:"Editors"}.';
COMMENT ON COLUMN "group".group_id IS 'Primary key. Example: 10.';
COMMENT ON COLUMN "group".name IS 'Unique group name. Example: "Reviewers".';

CREATE TABLE user_group (
                            user_id INT REFERENCES "user"(user_id) ON DELETE CASCADE,
                            group_id INT REFERENCES "group"(group_id) ON DELETE CASCADE,
                            PRIMARY KEY (user_id, group_id)
);
COMMENT ON TABLE user_group IS
    'Users-to-groups binding. Example: user 5 belongs to group 10.';
COMMENT ON COLUMN user_group.user_id IS 'FK to user. Example: 5.';
COMMENT ON COLUMN user_group.group_id IS 'FK to group. Example: 10.';

-- =========================================================
-- ACL
-- =========================================================
CREATE TABLE acl (
                     acl_id SERIAL PRIMARY KEY,
                     name TEXT
);
COMMENT ON TABLE acl IS
    'Reusable ACL container. Example: {acl_id:1, name:"Default Document ACL"}.';
COMMENT ON COLUMN acl.acl_id IS 'Primary key. Example: 1.';
COMMENT ON COLUMN acl.name IS 'Display name. Example: "Editors - Full access".';

CREATE TABLE acl_entry (
                           acl_entry_id SERIAL PRIMARY KEY,
                           acl_id INT NOT NULL REFERENCES acl(acl_id) ON DELETE CASCADE,
                           grantee_type grantee_type_enum NOT NULL,
                           grantee_id INT NOT NULL,
                           can_read BOOLEAN DEFAULT TRUE,
                           can_write BOOLEAN DEFAULT FALSE,
                           can_delete BOOLEAN DEFAULT FALSE,
                           can_change_acl BOOLEAN DEFAULT FALSE
);
COMMENT ON TABLE acl_entry IS
    'Permission entry within an ACL.
    Example: {acl_id:1, grantee_type:"GROUP", grantee_id:10, can_read:true, can_write:true}.';
COMMENT ON COLUMN acl_entry.acl_entry_id IS 'Primary key. Example: 100.';
COMMENT ON COLUMN acl_entry.acl_id IS 'FK to ACL. Example: 1.';
COMMENT ON COLUMN acl_entry.grantee_type IS 'USER/GROUP. Example: "GROUP".';
COMMENT ON COLUMN acl_entry.grantee_id IS 'User or group id. Example: 10.';
COMMENT ON COLUMN acl_entry.can_read IS 'Read permission. Example: TRUE.';
COMMENT ON COLUMN acl_entry.can_write IS 'Write permission. Example: TRUE.';
COMMENT ON COLUMN acl_entry.can_delete IS 'Delete permission. Example: FALSE.';
COMMENT ON COLUMN acl_entry.can_change_acl IS 'Manage ACL permission. Example: FALSE.';

CREATE INDEX idx_acl_entry_acl ON acl_entry(acl_id);
CREATE INDEX idx_acl_entry_grantee ON acl_entry(grantee_type, grantee_id);

-- =========================================================
-- OBJECT TYPES / CLASSES
-- =========================================================
CREATE TABLE object_type (
                             object_type_id SERIAL PRIMARY KEY,
                             name TEXT NOT NULL UNIQUE,
                             name_i18n JSONB DEFAULT '{}'::jsonb,
                             acl_id INT REFERENCES acl(acl_id) ON DELETE SET NULL
);
COMMENT ON TABLE object_type IS
    'Top-level category of objects.
    Examples: "Document", "Project", "Customer".';
COMMENT ON COLUMN object_type.object_type_id IS 'PK. Example: 1.';
COMMENT ON COLUMN object_type.name IS 'Unique type name. Example: "Document".';
COMMENT ON COLUMN object_type.name_i18n IS 'Localized labels. Example: {"en":"Document","ru":"Документ"}.';
COMMENT ON COLUMN object_type.acl_id IS 'Inherited ACL for this type. Example: 1.';

CREATE TABLE "class" (
                         class_id SERIAL PRIMARY KEY,
                         name TEXT NOT NULL,
                         description TEXT,
                         object_type_id INT NOT NULL REFERENCES object_type(object_type_id) ON DELETE RESTRICT,
                         is_active BOOLEAN DEFAULT TRUE,
                         acl_id INT REFERENCES acl(acl_id) ON DELETE SET NULL,
                         UNIQUE (object_type_id, name)
);
COMMENT ON TABLE "class" IS
    'Subclass within an object_type. Example: type="Document", class="Contract".';
COMMENT ON COLUMN "class".class_id IS 'PK. Example: 5.';
COMMENT ON COLUMN "class".name IS 'Class name. Example: "Contract".';
COMMENT ON COLUMN "class".description IS 'Optional description. Example: "Customer contracts".';
COMMENT ON COLUMN "class".object_type_id IS 'FK to object_type. Example: 1.';
COMMENT ON COLUMN "class".is_active IS 'TRUE/active; FALSE/archived. Example: TRUE.';
COMMENT ON COLUMN "class".acl_id IS 'Class-specific ACL. Example: 2.';

-- =========================================================
-- VALUE LISTS
-- =========================================================
CREATE TABLE value_list (
                            value_list_id SERIAL PRIMARY KEY,
                            name TEXT NOT NULL UNIQUE,
                            name_i18n JSONB DEFAULT '{}'::jsonb
);
COMMENT ON TABLE value_list IS
    'Dictionary/picklist used by VALUELIST properties. Example: "DocumentStatus".';
COMMENT ON COLUMN value_list.value_list_id IS 'PK. Example: 10.';
COMMENT ON COLUMN value_list.name IS 'Internal name. Example: "DocumentStatus".';
COMMENT ON COLUMN value_list.name_i18n IS 'Localized name. Example: {"en":"Status"}.';

CREATE TABLE value_list_item (
                                 item_id SERIAL PRIMARY KEY,
                                 value_list_id INT NOT NULL REFERENCES value_list(value_list_id) ON DELETE CASCADE,
                                 value_text TEXT NOT NULL,
                                 value_text_i18n JSONB DEFAULT '{}'::jsonb,
                                 sort_order INT,
                                 is_active BOOLEAN DEFAULT TRUE,
                                 parent_item_id INT REFERENCES value_list_item(item_id) ON DELETE SET NULL,
                                 external_code TEXT,
                                 UNIQUE(value_list_id, value_text)
);
COMMENT ON TABLE value_list_item IS
    'Single item within a value_list. Example: "Approved".';
COMMENT ON COLUMN value_list_item.item_id IS 'PK. Example: 101.';
COMMENT ON COLUMN value_list_item.value_list_id IS 'FK to list. Example: 10.';
COMMENT ON COLUMN value_list_item.value_text IS 'Display text. Example: "Approved".';
COMMENT ON COLUMN value_list_item.value_text_i18n IS 'Translations. Example: {"ru":"Согласовано"}.';
COMMENT ON COLUMN value_list_item.sort_order IS 'Ordering. Example: 10.';
COMMENT ON COLUMN value_list_item.is_active IS 'Visible flag. Example: TRUE.';
COMMENT ON COLUMN value_list_item.parent_item_id IS 'Hierarchy parent. Example: null.';
COMMENT ON COLUMN value_list_item.external_code IS 'Integration code. Example: "APPROVED_200".';

-- =========================================================
-- PROPERTY DEFINITIONS (EAV)
-- =========================================================
CREATE TABLE property_def (
                              property_def_id SERIAL PRIMARY KEY,
                              name TEXT NOT NULL,
                              caption_i18n JSONB DEFAULT '{}'::jsonb,
                              data_type data_type_enum NOT NULL,
                              ref_object_type_id INT REFERENCES object_type(object_type_id),
                              value_list_id INT REFERENCES value_list(value_list_id),
                              is_multiselect BOOLEAN DEFAULT FALSE,
                              is_required BOOLEAN DEFAULT FALSE,
                              is_unique  BOOLEAN DEFAULT FALSE,
                              regex TEXT,
                              default_value TEXT,
                              description TEXT
);
COMMENT ON TABLE property_def IS
    'Property catalog. Example: name="Status", data_type="VALUELIST", value_list_id=10.';
COMMENT ON COLUMN property_def.property_def_id IS 'PK. Example: 50.';
COMMENT ON COLUMN property_def.name IS 'Technical key. Example: "CustomerName".';
COMMENT ON COLUMN property_def.caption_i18n IS 'UI label(s). Example: {"en":"Customer Name"}.';
COMMENT ON COLUMN property_def.data_type IS 'TEXT/NUMBER/DATE/BOOLEAN/LOOKUP/VALUELIST. Example: "VALUELIST".';
COMMENT ON COLUMN property_def.ref_object_type_id IS 'For LOOKUP. Example: points to object_type "Customer".';
COMMENT ON COLUMN property_def.value_list_id IS 'For VALUELIST. Example: points "DocumentStatus".';
COMMENT ON COLUMN property_def.is_multiselect IS 'Allows multiple values. Example: TRUE.';
COMMENT ON COLUMN property_def.is_required IS 'Mandatory value. Example: TRUE.';
COMMENT ON COLUMN property_def.is_unique IS 'Unique across class scope (checked in app). Example: FALSE.';
COMMENT ON COLUMN property_def.regex IS 'Validation regex. Example: "^[A-Z0-9_-]{3,20}$".';
COMMENT ON COLUMN property_def.default_value IS 'Default as string. Example: "Draft".';
COMMENT ON COLUMN property_def.description IS 'Property purpose. Example: "Approval status".';

-- =========================================================
-- CLASS-PROPERTY BINDINGS
-- =========================================================
CREATE TABLE class_property (
                                class_id INT NOT NULL REFERENCES "class"(class_id) ON DELETE CASCADE,
                                property_def_id INT NOT NULL REFERENCES property_def(property_def_id) ON DELETE CASCADE,
                                is_readonly BOOLEAN DEFAULT FALSE,
                                is_hidden   BOOLEAN DEFAULT FALSE,
                                display_order INT,
                                PRIMARY KEY (class_id, property_def_id)
);
COMMENT ON TABLE class_property IS
    'Which properties are available in a class + UI hints. Example: Contract -> {Status, Customer, EffectiveDate}.';
COMMENT ON COLUMN class_property.class_id IS 'FK to class. Example: 5.';
COMMENT ON COLUMN class_property.property_def_id IS 'FK to property_def. Example: 50.';
COMMENT ON COLUMN class_property.is_readonly IS 'Read-only UI flag. Example: FALSE.';
COMMENT ON COLUMN class_property.is_hidden IS 'Hidden UI flag. Example: FALSE.';
COMMENT ON COLUMN class_property.display_order IS 'UI order. Example: 20.';

-- =========================================================
-- OBJECTS
-- =========================================================
CREATE TABLE "object" (
                          object_id SERIAL PRIMARY KEY,
                          object_type_id INT NOT NULL REFERENCES object_type(object_type_id),
                          class_id INT REFERENCES "class"(class_id),
                          name TEXT,
                          acl_id INT REFERENCES acl(acl_id) ON DELETE SET NULL,
                          is_deleted BOOLEAN DEFAULT FALSE,
                          deleted_at TIMESTAMP,
                          deleted_by INT REFERENCES "user"(user_id),
                          UNIQUE (object_type_id, class_id, name)
);
COMMENT ON TABLE "object" IS
    'Logical instance (e.g., a document or a project). Example: "Contract ACME-2025".';
COMMENT ON COLUMN "object".object_id IS 'PK. Example: 1001.';
COMMENT ON COLUMN "object".object_type_id IS 'FK type. Example: 1 ("Document").';
COMMENT ON COLUMN "object".class_id IS 'FK class. Example: 5 ("Contract").';
COMMENT ON COLUMN "object".name IS 'Human-readable title. Example: "Contract ACME 2025".';
COMMENT ON COLUMN "object".acl_id IS 'Object-level ACL. Example: 1.';
COMMENT ON COLUMN "object".is_deleted IS 'Soft delete flag. Example: FALSE.';
COMMENT ON COLUMN "object".deleted_at IS 'Soft deletion time. Example: 2025-06-03 14:15.';
COMMENT ON COLUMN "object".deleted_by IS 'Who soft-deleted. Example: user_id=2.';

-- =========================================================
-- OBJECT VERSIONS
-- =========================================================
CREATE TABLE object_version (
                                version_id SERIAL PRIMARY KEY,
                                object_id INT NOT NULL REFERENCES "object"(object_id) ON DELETE CASCADE,
                                version_num INT NOT NULL,
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                created_by INT REFERENCES "user"(user_id),
                                comment TEXT,
                                single_file BOOLEAN DEFAULT TRUE,
                                acl_id INT REFERENCES acl(acl_id) ON DELETE SET NULL,
                                is_locked BOOLEAN DEFAULT FALSE,
                                locked_by INT REFERENCES "user"(user_id),
                                locked_at TIMESTAMP,
                                UNIQUE (object_id, version_num)
);
COMMENT ON TABLE object_version IS
    'Version snapshot of an object. Example: version_num=3, comment="Updated appendix".';
COMMENT ON COLUMN object_version.version_id IS 'PK. Example: 2001.';
COMMENT ON COLUMN object_version.object_id IS 'FK to object. Example: 1001.';
COMMENT ON COLUMN object_version.version_num IS 'Sequential number. Example: 3.';
COMMENT ON COLUMN object_version.created_at IS 'Creation time. Example: 2025-02-01 09:15.';
COMMENT ON COLUMN object_version.modified_at IS 'Last modification time.';
COMMENT ON COLUMN object_version.created_by IS 'Author user id.';
COMMENT ON COLUMN object_version.comment IS 'Version comment. Example: "Fix typos".';
COMMENT ON COLUMN object_version.single_file IS 'If version expects a single file. Example: TRUE.';
COMMENT ON COLUMN object_version.acl_id IS 'Version-level ACL. Example: 4.';
COMMENT ON COLUMN object_version.is_locked IS 'Check-out lock flag. Example: FALSE.';
COMMENT ON COLUMN object_version.locked_by IS 'Lock owner user id.';
COMMENT ON COLUMN object_version.locked_at IS 'Lock time.';

CREATE INDEX idx_object_version_object ON object_version(object_id);

-- =========================================================
-- FILE STORAGE + FILES
-- =========================================================
CREATE TABLE file_storage (
                              storage_id SERIAL PRIMARY KEY,
                              kind storage_kind_enum NOT NULL,
                              name TEXT,
                              base_path TEXT,
                              bucket TEXT,
                              endpoint TEXT,
                              access_key TEXT,
                              secret_key TEXT
);
COMMENT ON TABLE file_storage IS
    'Physical storage config. Example: kind="S3", bucket="repo", endpoint="https://minio:9000".';
COMMENT ON COLUMN file_storage.storage_id IS 'PK. Example: 1.';
COMMENT ON COLUMN file_storage.kind IS 'DB/FS/S3. Example: "FS".';
COMMENT ON COLUMN file_storage.name IS 'Friendly name. Example: "Primary Store".';
COMMENT ON COLUMN file_storage.base_path IS 'FS root. Example: "/data/repo".';
COMMENT ON COLUMN file_storage.bucket IS 'S3 bucket. Example: "documents".';
COMMENT ON COLUMN file_storage.endpoint IS 'S3 endpoint. Example: "https://s3.local".';
COMMENT ON COLUMN file_storage.access_key IS 'S3 access key.';
COMMENT ON COLUMN file_storage.secret_key IS 'S3 secret key.';

CREATE TABLE object_file (
                             file_id SERIAL PRIMARY KEY,
                             object_version_id INT NOT NULL REFERENCES object_version(version_id) ON DELETE CASCADE,
                             file_name TEXT NOT NULL,
                             file_data BYTEA,
                             file_size BIGINT,
                             content_type TEXT,
                             external_file_path TEXT,
                             content_sha256 TEXT,
                             storage_id INT REFERENCES file_storage(storage_id),
                             inline BOOLEAN DEFAULT TRUE
);
COMMENT ON TABLE object_file IS
    'Attached files. Example: "contract_v3.pdf", 512KB, sha256="ab34ef...".';
COMMENT ON COLUMN object_file.file_id IS 'PK. Example: 5001.';
COMMENT ON COLUMN object_file.object_version_id IS 'FK to version. Example: 2001.';
COMMENT ON COLUMN object_file.file_name IS 'Original file name. Example: "contract.pdf".';
COMMENT ON COLUMN object_file.file_data IS 'Binary content if inline.';
COMMENT ON COLUMN object_file.file_size IS 'Size in bytes. Example: 512000.';
COMMENT ON COLUMN object_file.content_type IS 'MIME. Example: "application/pdf".';
COMMENT ON COLUMN object_file.external_file_path IS 'FS path/S3 key. Example: "/repo/2025/contract.pdf".';
COMMENT ON COLUMN object_file.content_sha256 IS 'Integrity/dedup hash. Example: "4d3a3b8e...".';
COMMENT ON COLUMN object_file.storage_id IS 'FK to file_storage. Example: 1.';
COMMENT ON COLUMN object_file.inline IS 'TRUE=DB BYTEA; FALSE=external storage. Example: FALSE.';

CREATE INDEX idx_object_file_version ON object_file(object_version_id);
CREATE INDEX idx_object_file_sha     ON object_file(content_sha256);

-- =========================================================
-- PROPERTY VALUES (EAV)
-- =========================================================
CREATE TABLE property_value (
                                property_value_id SERIAL PRIMARY KEY,
                                object_version_id INT NOT NULL REFERENCES object_version(version_id) ON DELETE CASCADE,
                                property_def_id INT NOT NULL REFERENCES property_def(property_def_id),
                                ordinal SMALLINT DEFAULT 0,
                                value_text TEXT,
                                value_number NUMERIC,
                                value_date TIMESTAMP,
                                value_boolean BOOLEAN,
                                ref_object_id INT REFERENCES "object"(object_id),
                                value_list_item_id INT REFERENCES value_list_item(item_id),
                                computed BOOLEAN DEFAULT FALSE,
                                source TEXT,
                                UNIQUE (object_version_id, property_def_id, ordinal)
);
COMMENT ON TABLE property_value IS
    'EAV values per version. Examples: value_text="ACME Ltd.", value_list_item_id=15 ("Approved").';
COMMENT ON COLUMN property_value.property_value_id IS 'PK. Example: 9001.';
COMMENT ON COLUMN property_value.object_version_id IS 'FK to version. Example: 2001.';
COMMENT ON COLUMN property_value.property_def_id IS 'FK to property_def. Example: 50.';
COMMENT ON COLUMN property_value.ordinal IS '0-based order for multivalue. Example: 0.';
COMMENT ON COLUMN property_value.value_text IS 'Text value. Example: "Active".';
COMMENT ON COLUMN property_value.value_number IS 'Numeric value. Example: 1200.5.';
COMMENT ON COLUMN property_value.value_date IS 'Date/time value. Example: 2025-03-20.';
COMMENT ON COLUMN property_value.value_boolean IS 'Boolean value. Example: TRUE.';
COMMENT ON COLUMN property_value.ref_object_id IS 'LOOKUP target object id. Example: 1005.';
COMMENT ON COLUMN property_value.value_list_item_id IS 'VALUELIST item id. Example: 15.';
COMMENT ON COLUMN property_value.computed IS 'Derived flag. Example: FALSE.';
COMMENT ON COLUMN property_value.source IS 'Formula/source. Example: "propA + propB".';

CREATE INDEX idx_propval_text   ON property_value(property_def_id, value_text);
CREATE INDEX idx_propval_num    ON property_value(property_def_id, value_number);
CREATE INDEX idx_propval_date   ON property_value(property_def_id, value_date);
CREATE INDEX idx_propval_bool   ON property_value(property_def_id, value_boolean);
CREATE INDEX idx_propval_vlist  ON property_value(property_def_id, value_list_item_id);
CREATE INDEX idx_propval_lookup ON property_value(property_def_id, ref_object_id);

-- =========================================================
-- OBJECT LINKS (RELATIONSHIPS)
-- =========================================================
CREATE TABLE object_link (
                             link_id SERIAL PRIMARY KEY,
                             src_object_id INT NOT NULL REFERENCES "object"(object_id) ON DELETE CASCADE,
                             dst_object_id INT NOT NULL REFERENCES "object"(object_id) ON DELETE CASCADE,
                             role TEXT NOT NULL,
                             direction link_direction_enum DEFAULT 'UNI',
                             created_at TIMESTAMP DEFAULT NOW(),
                             created_by INT REFERENCES "user"(user_id),
                             UNIQUE (src_object_id, dst_object_id, role)
);
COMMENT ON TABLE object_link IS
    'Explicit object-to-object relationships. Example: Contract -> Customer (role="Customer").';
COMMENT ON COLUMN object_link.link_id IS 'PK. Example: 701.';
COMMENT ON COLUMN object_link.src_object_id IS 'Source object id. Example: 1001.';
COMMENT ON COLUMN object_link.dst_object_id IS 'Target object id. Example: 2002.';
COMMENT ON COLUMN object_link.role IS 'Relation role. Example: "Customer".';
COMMENT ON COLUMN object_link.direction IS 'UNI or BI. Example: "UNI".';
COMMENT ON COLUMN object_link.created_at IS 'Creation time. Example: 2025-02-01.';
COMMENT ON COLUMN object_link.created_by IS 'User who created link. Example: 5.';

CREATE INDEX idx_object_link_src ON object_link(src_object_id);
CREATE INDEX idx_object_link_dst ON object_link(dst_object_id);

-- =========================================================
-- WORKFLOW
-- =========================================================
CREATE TABLE workflow (
                          workflow_id SERIAL PRIMARY KEY,
                          name TEXT NOT NULL UNIQUE,
                          description TEXT
);
COMMENT ON TABLE workflow IS
    'Workflow definition. Example: "Document Approval".';
COMMENT ON COLUMN workflow.workflow_id IS 'PK. Example: 30.';
COMMENT ON COLUMN workflow.name IS 'Workflow name. Example: "Approval".';
COMMENT ON COLUMN workflow.description IS 'Details. Example: "Approval process for contracts".';

CREATE TABLE workflow_state (
                                state_id SERIAL PRIMARY KEY,
                                workflow_id INT NOT NULL REFERENCES workflow(workflow_id) ON DELETE CASCADE,
                                name TEXT NOT NULL,
                                is_initial BOOLEAN DEFAULT FALSE,
                                is_final BOOLEAN DEFAULT FALSE,
                                UNIQUE (workflow_id, name)
);
COMMENT ON TABLE workflow_state IS
    'States within a workflow. Example: Draft, In Review, Approved.';
COMMENT ON COLUMN workflow_state.state_id IS 'PK. Example: 301.';
COMMENT ON COLUMN workflow_state.workflow_id IS 'FK to workflow. Example: 30.';
COMMENT ON COLUMN workflow_state.name IS 'State name. Example: "Draft".';
COMMENT ON COLUMN workflow_state.is_initial IS 'Start state flag. Example: TRUE.';
COMMENT ON COLUMN workflow_state.is_final IS 'Terminal state flag. Example: FALSE.';

CREATE TABLE workflow_transition (
                                     transition_id SERIAL PRIMARY KEY,
                                     workflow_id INT NOT NULL REFERENCES workflow(workflow_id) ON DELETE CASCADE,
                                     from_state_id INT NOT NULL REFERENCES workflow_state(state_id),
                                     to_state_id INT NOT NULL REFERENCES workflow_state(state_id),
                                     name TEXT,
                                     condition_json JSONB
);
COMMENT ON TABLE workflow_transition IS
    'Allowed transitions. Example: Draft->Review (name="Submit", condition_json={"role":"Editor"}).';
COMMENT ON COLUMN workflow_transition.transition_id IS 'PK. Example: 401.';
COMMENT ON COLUMN workflow_transition.workflow_id IS 'FK to workflow. Example: 30.';
COMMENT ON COLUMN workflow_transition.from_state_id IS 'From state id. Example: 301.';
COMMENT ON COLUMN workflow_transition.to_state_id IS 'To state id. Example: 302.';
COMMENT ON COLUMN workflow_transition.name IS 'Transition name. Example: "Approve".';
COMMENT ON COLUMN workflow_transition.condition_json IS 'Optional constraints JSON.';

CREATE TABLE object_workflow_state (
                                       object_id INT PRIMARY KEY REFERENCES "object"(object_id) ON DELETE CASCADE,
                                       state_id INT REFERENCES workflow_state(state_id),
                                       changed_at TIMESTAMP DEFAULT NOW(),
                                       changed_by INT REFERENCES "user"(user_id)
);
COMMENT ON TABLE object_workflow_state IS
    'Current workflow state per object. Example: object 1001 -> state Approved.';
COMMENT ON COLUMN object_workflow_state.object_id IS 'FK to object. Example: 1001.';
COMMENT ON COLUMN object_workflow_state.state_id IS 'FK to workflow_state. Example: 302.';
COMMENT ON COLUMN object_workflow_state.changed_at IS 'When changed. Example: 2025-02-01 10:10.';
COMMENT ON COLUMN object_workflow_state.changed_by IS 'Who changed. Example: user 5.';

-- =========================================================
-- VIEWS / GROUPINGS
-- =========================================================
CREATE TABLE object_view (
                             view_id SERIAL PRIMARY KEY,
                             name TEXT NOT NULL,
                             is_common BOOLEAN DEFAULT FALSE,
                             created_by INT REFERENCES "user"(user_id),
                             filter_json JSONB,
                             sort_order INT
);
COMMENT ON TABLE object_view IS
    'Virtual folders / saved searches. Example: "My Active Contracts".';
COMMENT ON COLUMN object_view.view_id IS 'PK. Example: 8001.';
COMMENT ON COLUMN object_view.name IS 'View name. Example: "Pending Approvals".';
COMMENT ON COLUMN object_view.is_common IS 'TRUE=shared; FALSE=private. Example: TRUE.';
COMMENT ON COLUMN object_view.created_by IS 'Creator user id.';
COMMENT ON COLUMN object_view.filter_json IS 'JSON filter array. Example: [{"property_def_id":50,"op":"=","value":"Active"}].';
COMMENT ON COLUMN object_view.sort_order IS 'Ordering index. Example: 10.';

CREATE TABLE object_view_grouping (
                                      view_id INT REFERENCES object_view(view_id) ON DELETE CASCADE,
                                      level INT NOT NULL,
                                      property_def_id INT NOT NULL REFERENCES property_def(property_def_id),
                                      PRIMARY KEY (view_id, level)
);
COMMENT ON TABLE object_view_grouping IS
    'Hierarchical grouping configuration for a view. Example: level 0=Customer, level 1=Status.';
COMMENT ON COLUMN object_view_grouping.view_id IS 'FK to object_view.';
COMMENT ON COLUMN object_view_grouping.level IS 'Level order. Example: 0.';
COMMENT ON COLUMN object_view_grouping.property_def_id IS 'Group-by property. Example: 50.';

CREATE INDEX idx_object_view_filter ON object_view USING GIN (filter_json);

-- =========================================================
-- AUDIT
-- =========================================================
CREATE TABLE object_version_audit (
                                      audit_id SERIAL PRIMARY KEY,
                                      version_id INT NOT NULL REFERENCES object_version(version_id) ON DELETE CASCADE,
                                      modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                      modified_by BIGINT REFERENCES "user"(user_id),
                                      change_type TEXT NOT NULL,
                                      field_changed TEXT,
                                      old_value TEXT,
                                      new_value TEXT,
                                      change_summary TEXT
);
COMMENT ON TABLE object_version_audit IS
    'Change history for versions. Example: property_change "Status": Draft->Approved.';
COMMENT ON COLUMN object_version_audit.audit_id IS 'PK. Example: 90001.';
COMMENT ON COLUMN object_version_audit.version_id IS 'FK to version. Example: 2001.';
COMMENT ON COLUMN object_version_audit.modified_at IS 'Change time.';
COMMENT ON COLUMN object_version_audit.modified_by IS 'Who changed.';
COMMENT ON COLUMN object_version_audit.change_type IS 'Type: property_change/file_update/lock/unlock/etc.';
COMMENT ON COLUMN object_version_audit.field_changed IS 'Affected field. Example: "Status".';
COMMENT ON COLUMN object_version_audit.old_value IS 'Previous value. Example: "Draft".';
COMMENT ON COLUMN object_version_audit.new_value IS 'New value. Example: "Approved".';
COMMENT ON COLUMN object_version_audit.change_summary IS 'Free text / JSON summary.';

CREATE INDEX idx_ova_version_time ON object_version_audit(version_id, modified_at);

-- =========================================================
-- SEARCH TEXT CACHE (optional FTS)
-- =========================================================
CREATE TABLE search_text_cache (
                                   object_version_id INT PRIMARY KEY REFERENCES object_version(version_id) ON DELETE CASCADE,
                                   extracted_text TSVECTOR,
                                   updated_at TIMESTAMP DEFAULT NOW()
);
COMMENT ON TABLE search_text_cache IS
    'Per-version FTS vector with extracted (Tika/OCR) text for full-text search.';
COMMENT ON COLUMN search_text_cache.object_version_id IS 'FK to version. Example: 2001.';
COMMENT ON COLUMN search_text_cache.extracted_text IS 'tsvector content for Postgres FTS.';
COMMENT ON COLUMN search_text_cache.updated_at IS 'Refresh timestamp.';
CREATE INDEX idx_search_text_cache_fts ON search_text_cache USING GIN (extracted_text);

COMMIT;
