ALTER TABLE campaign_difficulties
    DROP CONSTRAINT IF EXISTS campaign_difficulties_requirement_type_check;

ALTER TABLE campaign_difficulties
    ADD CONSTRAINT campaign_difficulties_requirement_type_check
        CHECK (requirement_type IN ('ACC', 'AP', 'SCORE', 'STREAK_115', 'FC', 'RANK', 'PASS',
            'COMBO', 'BOMB_HITS', 'MISTAKES', 'PAUSES'));

ALTER TABLE campaign_difficulties
    DROP CONSTRAINT IF EXISTS campaign_difficulties_barrier_condition_type_check;

ALTER TABLE campaign_difficulties
    ADD CONSTRAINT campaign_difficulties_barrier_condition_type_check
        CHECK (barrier_condition_type IN ('AVERAGE_ACC', 'AVERAGE_AP', 'AP_MAX', 'ACC_MAX',
            'STREAK_115_AVERAGE', 'STREAK_115_MAX', 'FC', 'AVERAGE_RANK', 'MAX_RANK',
            'COMPLETION_COUNT', 'PASS', 'AVERAGE_COMBO', 'AVERAGE_BOMB_HITS', 'AVERAGE_MISTAKES',
            'AVERAGE_PAUSES'));

ALTER TABLE campaign_difficulty_targets
    DROP CONSTRAINT IF EXISTS campaign_difficulty_targets_type_check;

ALTER TABLE campaign_difficulty_targets
    ADD CONSTRAINT campaign_difficulty_targets_type_check
        CHECK (requirement_type IN ('ACC', 'AP', 'SCORE', 'STREAK_115', 'FC', 'RANK', 'PASS',
            'COMBO', 'BOMB_HITS', 'MISTAKES', 'PAUSES'));
