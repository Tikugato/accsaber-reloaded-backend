ALTER TABLE campaigns
    ADD COLUMN background_size INTEGER,
    ADD COLUMN background_x    INTEGER,
    ADD COLUMN background_y    INTEGER;

ALTER TABLE campaigns
    ADD CONSTRAINT campaigns_background_placement_shape
        CHECK (num_nonnulls(background_size, background_x, background_y) IN (0, 3));

ALTER TABLE campaigns
    ADD CONSTRAINT campaigns_background_size_range
        CHECK (background_size IS NULL OR background_size BETWEEN 1 AND 1000);

ALTER TABLE campaigns
    ADD CONSTRAINT campaigns_background_offset_range
        CHECK ((background_x IS NULL OR background_x BETWEEN -1000 AND 1000)
        AND (background_y IS NULL OR background_y BETWEEN -1000 AND 1000));
