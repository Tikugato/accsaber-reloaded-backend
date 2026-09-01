CREATE UNIQUE INDEX idx_user_crate_opens_consumed_link
    ON user_crate_opens ((consumed_link_id::text));

CREATE VIEW user_item_crate_sources AS
SELECT consumed_link_id::text AS source_id,
       crate_item_id
FROM user_crate_opens;
