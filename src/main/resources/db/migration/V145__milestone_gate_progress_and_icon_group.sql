ALTER TABLE user_milestone_links
    ADD COLUMN gate_fraction DOUBLE PRECISION;

ALTER TABLE milestones
    ADD COLUMN icon_group TEXT;

UPDATE milestones SET icon_group = CASE
    WHEN query_spec->>'from' = 'crate_opens' THEN 'CRATE'
    WHEN query_spec->>'from' = 'disintegrations' THEN 'ESSENCE'
    WHEN query_spec->>'from' IN ('trades', 'trades_received') THEN 'TRADE'
    WHEN query_spec->>'from' IN ('market_listings', 'market_purchases') THEN 'MARKET'
    WHEN query_spec->>'from' IN ('user_items', 'user_item_modifiers') THEN 'ITEM'
    WHEN query_spec->>'from' IN ('user_campaigns', 'user_campaign_scores', 'authored_campaigns') THEN 'CAMPAIGN'
    WHEN query_spec->>'from' = 'user_missions'
         AND query_spec->'select'->>'column' = 'xp_reward' THEN 'XP'
    WHEN query_spec->>'from' = 'user_missions' THEN 'MISSION'
    WHEN query_spec->>'from' = 'user_milestone_links'
         AND query_spec->'select'->>'column' = 'milestone_xp' THEN 'XP'
    WHEN query_spec->>'from' = 'user_milestone_links' THEN 'MILESTONE'
    WHEN query_spec->>'from' = 'users'
         AND query_spec->'select'->>'column' = 'total_xp' THEN 'XP'
    WHEN query_spec->>'from' = 'users'
         AND query_spec->'select'->>'column' = 'net_worth' THEN 'ESSENCE'
    WHEN query_spec->>'from' = 'users' THEN 'PLAYER'
    WHEN query_spec->>'from' = 'user_category_statistics'
         AND query_spec->'select'->>'column' IN ('ranking', 'country_ranking') THEN 'RANK'
    WHEN query_spec->>'from' = 'user_category_statistics'
         AND query_spec->'select'->>'column' IN ('ap', 'average_ap') THEN 'AP'
    WHEN query_spec->>'from' = 'user_category_statistics'
         AND query_spec->'select'->>'column' = 'average_acc' THEN 'ACCURACY'
    WHEN query_spec->>'from' = 'user_category_statistics' THEN 'PLAYER'
    WHEN query_spec->>'from' = 'scores'
         AND query_spec->'select'->>'column' LIKE 'map_difficulty%' THEN 'MAP'
    WHEN query_spec->>'from' = 'scores'
         AND query_spec->'select'->>'column' IN ('ap', 'weighted_ap') THEN 'AP'
    WHEN query_spec->>'from' = 'scores'
         AND query_spec->'select'->>'column' = 'accuracy' THEN 'ACCURACY'
    WHEN query_spec->>'from' = 'scores'
         AND query_spec->'select'->>'column' = 'streak_115' THEN 'STREAK'
    WHEN query_spec->>'from' = 'scores'
         AND query_spec->'select'->>'column' IN ('rank', 'rank_when_set') THEN 'RANK'
    WHEN query_spec->>'from' = 'scores'
         AND query_spec->'select'->>'column' IN ('bomb_hits', 'wall_hits', 'pauses', 'misses', 'bad_cuts') THEN 'MISTAKE'
    WHEN query_spec->>'from' = 'scores' THEN 'SCORE'
    ELSE 'GENERAL'
END
WHERE icon_group IS NULL;

ALTER TABLE milestones
    ALTER COLUMN icon_group SET DEFAULT 'GENERAL',
    ALTER COLUMN icon_group SET NOT NULL;

CREATE INDEX idx_milestones_icon_group ON milestones(icon_group);

CREATE TABLE user_pinned_milestones (
    id            UUID         PRIMARY KEY DEFAULT uuidv7(),
    user_id       BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    milestone_id  UUID         NOT NULL REFERENCES milestones(id) ON DELETE CASCADE,
    display_order INTEGER      NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, milestone_id),
    UNIQUE (user_id, display_order)
);

CREATE INDEX idx_user_pinned_milestones_user ON user_pinned_milestones(user_id);
CREATE INDEX idx_user_pinned_milestones_milestone ON user_pinned_milestones(milestone_id);

CREATE TRIGGER trg_user_pinned_milestones_updated_at
    BEFORE UPDATE ON user_pinned_milestones
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
