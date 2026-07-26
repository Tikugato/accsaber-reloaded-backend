CREATE TABLE campaign_difficulty_modifiers (
    campaign_difficulty_id UUID        NOT NULL REFERENCES campaign_difficulties(id),
    modifier_id            UUID        NOT NULL REFERENCES modifiers(id),
    requirement            VARCHAR(16) NOT NULL,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (campaign_difficulty_id, modifier_id),
    CONSTRAINT campaign_difficulty_modifiers_requirement_check
        CHECK (requirement IN ('REQUIRED', 'FORBIDDEN'))
);

CREATE INDEX idx_campaign_difficulty_modifiers_modifier ON campaign_difficulty_modifiers(modifier_id);
