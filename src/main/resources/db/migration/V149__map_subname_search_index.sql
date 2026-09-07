CREATE INDEX IF NOT EXISTS idx_maps_search_song_subname_trgm
    ON maps USING gin (search_normalize(song_subname) gin_trgm_ops);
