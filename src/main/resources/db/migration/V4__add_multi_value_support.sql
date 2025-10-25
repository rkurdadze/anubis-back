-- =========================================================
-- V4__add_multi_value_support.sql
-- Purpose: Enable multi-select ValueList properties
-- =========================================================

BEGIN;

CREATE TABLE IF NOT EXISTS property_value_multi
(
    id                  BIGSERIAL PRIMARY KEY,
    property_value_id   BIGINT NOT NULL REFERENCES property_value(property_value_id) ON DELETE CASCADE,
    value_list_item_id  BIGINT NOT NULL REFERENCES value_list_item(item_id) ON DELETE CASCADE,
    UNIQUE (property_value_id, value_list_item_id)
);

COMMENT ON TABLE property_value_multi IS
    'Stores multiple selected ValueList items for properties with is_multiselect = true.';

COMMENT ON COLUMN property_value_multi.property_value_id IS
    'References the base property_value entry for which multiple ValueList items are stored.';

COMMENT ON COLUMN property_value_multi.value_list_item_id IS
    'References the value_list_item.item_id selected for a multi-select property.';

COMMIT;
