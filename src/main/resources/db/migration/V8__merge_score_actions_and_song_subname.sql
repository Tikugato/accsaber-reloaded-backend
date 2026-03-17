CREATE TABLE merge_score_actions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    link_id UUID NOT NULL REFERENCES users_duplicate_links(id) ON DELETE CASCADE,
    action_type VARCHAR(30) NOT NULL,
    score_id UUID NOT NULL REFERENCES scores(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_merge_score_actions_link ON merge_score_actions(link_id);

ALTER TABLE maps ADD COLUMN song_subname VARCHAR(255);