ALTER TABLE campaign_difficulties
    ALTER COLUMN position_x TYPE NUMERIC USING position_x::NUMERIC,
    ALTER COLUMN position_y TYPE NUMERIC USING position_y::NUMERIC;

ALTER TABLE campaign_texts
    ALTER COLUMN position_x TYPE NUMERIC USING position_x::NUMERIC,
    ALTER COLUMN position_y TYPE NUMERIC USING position_y::NUMERIC;
