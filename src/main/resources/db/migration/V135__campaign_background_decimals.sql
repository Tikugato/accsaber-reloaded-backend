ALTER TABLE campaigns
    ALTER COLUMN background_size TYPE NUMERIC USING background_size::NUMERIC,
    ALTER COLUMN background_x TYPE NUMERIC USING background_x::NUMERIC,
    ALTER COLUMN background_y TYPE NUMERIC USING background_y::NUMERIC;
