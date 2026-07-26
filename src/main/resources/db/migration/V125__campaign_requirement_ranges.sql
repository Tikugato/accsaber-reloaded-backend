ALTER TABLE campaign_difficulties
    ADD COLUMN requirement_value_max       NUMERIC(20,6),
    ADD COLUMN barrier_condition_value_max NUMERIC(20,6);

ALTER TABLE campaign_difficulties
    DROP CONSTRAINT IF EXISTS campaign_difficulties_requirement_type_check;

ALTER TABLE campaign_difficulties
    ADD CONSTRAINT campaign_difficulties_requirement_type_check
        CHECK (requirement_type IN ('ACC', 'AP', 'SCORE', 'STREAK_115', 'FC', 'RANK', 'PASS',
            'COMBO', 'BOMB_HITS'));

ALTER TABLE campaign_difficulties
    DROP CONSTRAINT IF EXISTS campaign_difficulties_barrier_condition_type_check;

ALTER TABLE campaign_difficulties
    ADD CONSTRAINT campaign_difficulties_barrier_condition_type_check
        CHECK (barrier_condition_type IN ('AVERAGE_ACC', 'AVERAGE_AP', 'AP_MAX', 'ACC_MAX',
            'STREAK_115_AVERAGE', 'STREAK_115_MAX', 'FC', 'AVERAGE_RANK', 'MAX_RANK',
            'COMPLETION_COUNT', 'PASS', 'AVERAGE_COMBO', 'AVERAGE_BOMB_HITS'));

ALTER TABLE campaign_difficulties
    DROP CONSTRAINT IF EXISTS campaign_difficulties_kind_shape;

ALTER TABLE campaign_difficulties
    ADD CONSTRAINT campaign_difficulties_kind_shape CHECK (
        (barrier = false AND map_difficulty_id IS NOT NULL
            AND requirement_type IS NOT NULL
            AND (requirement_value IS NOT NULL OR requirement_value_max IS NOT NULL))
        OR (barrier = true AND barrier_condition_type IS NOT NULL));
