ALTER TABLE user_missions
    ADD COLUMN assigned_skill_level     DOUBLE PRECISION,
    ADD COLUMN assigned_skill_threshold DOUBLE PRECISION;

CREATE INDEX idx_user_missions_template_assigned
    ON user_missions(template_id, assigned_at DESC);

CREATE INDEX idx_user_missions_pool_assigned
    ON user_missions(pool, assigned_at DESC);
