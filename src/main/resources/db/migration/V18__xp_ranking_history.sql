CREATE TABLE user_xp_ranking_history (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         BIGINT NOT NULL REFERENCES users(id),
    xp_ranking      INTEGER,
    xp_country_ranking INTEGER,
    recorded_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_xp_rank_history_user_recorded
    ON user_xp_ranking_history(user_id, recorded_at DESC);
