ALTER TABLE object_link
    ADD CONSTRAINT uk_object_link_unique
        UNIQUE (src_object_id, dst_object_id, role_id);