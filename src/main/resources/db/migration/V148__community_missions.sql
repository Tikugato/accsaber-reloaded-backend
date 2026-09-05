ALTER TABLE mission_templates DROP CONSTRAINT chk_mission_template_pool;
ALTER TABLE mission_templates ADD CONSTRAINT chk_mission_template_pool
    CHECK (pool IN ('daily', 'weekly', 'event', 'community'));

ALTER TABLE mission_templates DROP CONSTRAINT chk_mission_templates_event_pool;
ALTER TABLE mission_templates ADD CONSTRAINT chk_mission_templates_event_pool
    CHECK (event_id IS NULL OR pool IN ('event', 'community'));

ALTER TABLE mission_templates DROP CONSTRAINT chk_mission_templates_event_only_types;
ALTER TABLE mission_templates ADD CONSTRAINT chk_mission_templates_fixed_target_types CHECK (
    type NOT IN ('STREAK_SUM_N', 'SNIPE_RIVAL_ANY_MAP', 'AP_GAIN_OVERALL', 'BATCH_PLAY_N',
                 'PB_RANKED_BEFORE_N', 'CAMPAIGN_COMPLETE_N')
    OR pool IN ('event', 'community')
);

ALTER TABLE mission_templates ADD CONSTRAINT chk_mission_templates_community_window
    CHECK (pool <> 'community' OR event_id IS NOT NULL OR completable_until IS NOT NULL);

ALTER TABLE user_missions ALTER COLUMN user_id DROP NOT NULL;

ALTER TABLE user_missions DROP CONSTRAINT chk_user_missions_pool;
ALTER TABLE user_missions ADD CONSTRAINT chk_user_missions_pool
    CHECK (pool IN ('daily', 'weekly', 'event', 'community'));

ALTER TABLE user_missions ADD CONSTRAINT chk_user_missions_community_owner
    CHECK ((pool = 'community') = (user_id IS NULL));

CREATE UNIQUE INDEX uq_user_missions_one_open_community
    ON user_missions(template_id)
    WHERE user_id IS NULL AND status = 'active';

CREATE INDEX idx_user_missions_community_active
    ON user_missions(status) WHERE user_id IS NULL;

CREATE TABLE community_mission_contributions (
    user_mission_id UUID             NOT NULL REFERENCES user_missions(id) ON DELETE CASCADE,
    user_id         BIGINT           NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    contribution    DOUBLE PRECISION NOT NULL DEFAULT 0,
    first_at        TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    last_at         TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    rewarded_at     TIMESTAMPTZ,

    PRIMARY KEY (user_mission_id, user_id),
    CONSTRAINT chk_community_contribution_non_negative CHECK (contribution >= 0)
);

CREATE INDEX idx_community_contributions_leaderboard
    ON community_mission_contributions(user_mission_id, contribution DESC, first_at ASC);

CREATE INDEX idx_community_contributions_payout
    ON community_mission_contributions(user_mission_id, first_at ASC) WHERE rewarded_at IS NULL;

CREATE INDEX idx_community_contributions_user
    ON community_mission_contributions(user_id);
