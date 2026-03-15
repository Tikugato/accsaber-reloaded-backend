CREATE TABLE user_name_history (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     BIGINT          NOT NULL REFERENCES users(id),
    name        TEXT            NOT NULL,
    changed_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_name_history_user_id ON user_name_history(user_id);
CREATE INDEX idx_user_name_history_name ON user_name_history(name);
