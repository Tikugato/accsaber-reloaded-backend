CREATE TABLE campaign_difficulty_targets (
    id                     UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_difficulty_id UUID        NOT NULL REFERENCES campaign_difficulties(id),
    requirement_type       TEXT        NOT NULL,
    requirement_value      NUMERIC(20,6),
    requirement_value_max  NUMERIC(20,6),
    ordinal                INTEGER     NOT NULL DEFAULT 0,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT campaign_difficulty_targets_type_check
        CHECK (requirement_type IN ('ACC', 'AP', 'SCORE', 'STREAK_115', 'FC', 'RANK', 'PASS',
            'COMBO', 'BOMB_HITS')),
    CONSTRAINT campaign_difficulty_targets_bound_present
        CHECK (requirement_value IS NOT NULL OR requirement_value_max IS NOT NULL),
    CONSTRAINT campaign_difficulty_targets_bound_order
        CHECK (requirement_value IS NULL OR requirement_value_max IS NULL
            OR requirement_type IN ('RANK')
            OR requirement_value <= requirement_value_max),
    CONSTRAINT campaign_difficulty_targets_ordinal_unique
        UNIQUE (campaign_difficulty_id, ordinal)
);

CREATE INDEX idx_campaign_difficulty_targets_difficulty
    ON campaign_difficulty_targets(campaign_difficulty_id);

ALTER TABLE campaign_difficulties
    ADD COLUMN target_mode TEXT NOT NULL DEFAULT 'AND'
        CHECK (target_mode IN ('AND', 'OR'));

INSERT INTO campaign_difficulty_targets
    (campaign_difficulty_id, requirement_type, requirement_value, requirement_value_max, ordinal)
SELECT id, requirement_type, requirement_value, requirement_value_max, 0
FROM campaign_difficulties
WHERE barrier = false
  AND requirement_type IS NOT NULL
  AND (requirement_value IS NOT NULL OR requirement_value_max IS NOT NULL);

DO $$
DECLARE
    expected BIGINT;
    migrated BIGINT;
    orphaned BIGINT;
BEGIN
    SELECT count(*) INTO expected
    FROM campaign_difficulties
    WHERE barrier = false AND requirement_type IS NOT NULL;

    SELECT count(*) INTO migrated FROM campaign_difficulty_targets;

    SELECT count(*) INTO orphaned
    FROM campaign_difficulties cd
    WHERE cd.barrier = false
      AND cd.requirement_type IS NOT NULL
      AND NOT EXISTS (SELECT 1 FROM campaign_difficulty_targets t
                      WHERE t.campaign_difficulty_id = cd.id);

    IF expected <> migrated OR orphaned <> 0 THEN
        RAISE EXCEPTION
            'campaign target backfill incomplete: % nodes, % targets, % without a target',
            expected, migrated, orphaned;
    END IF;
END $$;
