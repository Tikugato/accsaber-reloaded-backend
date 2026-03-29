ALTER TABLE milestones DROP CONSTRAINT milestones_tier_check;
ALTER TABLE milestones ADD CONSTRAINT milestones_tier_check
    CHECK (tier IN ('bronze', 'silver', 'gold', 'platinum', 'diamond', 'apex'));

CREATE TABLE milestone_set_groups (
    id          UUID PRIMARY KEY DEFAULT uuidv7(),
    name        TEXT NOT NULL,
    description TEXT,
    active      BOOLEAN     NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE milestone_set_links (
    id         UUID PRIMARY KEY DEFAULT uuidv7(),
    group_id   UUID        NOT NULL REFERENCES milestone_set_groups(id),
    set_id     UUID        NOT NULL REFERENCES milestone_sets(id),
    sort_order INTEGER     NOT NULL DEFAULT 0,
    active     BOOLEAN     NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT unique_group_set UNIQUE (group_id, set_id)
);

CREATE INDEX idx_milestone_set_links_group ON milestone_set_links (group_id) WHERE active = true;
CREATE INDEX idx_milestone_set_links_set ON milestone_set_links (set_id) WHERE active = true;

CREATE TABLE user_relations (
    id             UUID PRIMARY KEY DEFAULT uuidv7(),
    user_id        BIGINT      NOT NULL REFERENCES users(id),
    target_user_id BIGINT      NOT NULL REFERENCES users(id),
    type           TEXT        NOT NULL CHECK (type IN ('follower', 'rival', 'blocked')),
    active         BOOLEAN     NOT NULL DEFAULT true,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT unique_user_relation_type UNIQUE (user_id, target_user_id, type),
    CONSTRAINT no_self_relation CHECK (user_id != target_user_id)
);

CREATE INDEX idx_user_relations_user ON user_relations (user_id) WHERE active = true;
CREATE INDEX idx_user_relations_target ON user_relations (target_user_id) WHERE active = true;
CREATE INDEX idx_user_relations_type ON user_relations (user_id, type) WHERE active = true;

CREATE TABLE user_oauth_links (
    id               UUID         PRIMARY KEY DEFAULT uuidv7(),
    user_id          BIGINT       NOT NULL REFERENCES users(id),
    provider         TEXT         NOT NULL CHECK (provider IN ('beatleader', 'steam')),
    provider_user_id VARCHAR(255) NOT NULL,
    access_token     TEXT,
    refresh_token    TEXT,
    token_expires_at TIMESTAMPTZ,
    active           BOOLEAN      NOT NULL DEFAULT true,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT unique_provider_user UNIQUE (provider, provider_user_id),
    CONSTRAINT unique_user_provider UNIQUE (user_id, provider)
);

CREATE INDEX idx_user_oauth_links_user ON user_oauth_links (user_id) WHERE active = true;
CREATE INDEX idx_user_oauth_links_provider_user ON user_oauth_links (provider, provider_user_id);

ALTER TABLE users RENAME COLUMN ss_inactive TO player_inactive;
