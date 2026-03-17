CREATE INDEX idx_ucs_category_active_ap
    ON user_category_statistics(category_id, ap DESC)
    WHERE active = true;

CREATE INDEX idx_users_active_country_notnull
    ON users(id, country)
    WHERE active = true AND country IS NOT NULL;
