ALTER TABLE map_difficulties
    ADD COLUMN bpm      DOUBLE PRECISION,
    ADD COLUMN notes    INTEGER,
    ADD COLUMN bombs    INTEGER,
    ADD COLUMN walls    INTEGER,
    ADD COLUMN duration INTEGER;
