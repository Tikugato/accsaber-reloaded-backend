CREATE EXTENSION IF NOT EXISTS unaccent WITH SCHEMA public;

CREATE OR REPLACE FUNCTION search_normalize(input TEXT)
RETURNS TEXT
LANGUAGE sql
IMMUTABLE
PARALLEL SAFE
RETURNS NULL ON NULL INPUT
AS $$ SELECT lower(public.unaccent('public.unaccent'::regdictionary, input)) $$;

CREATE OR REPLACE VIEW user_search_names AS
SELECT u.id AS user_id, search_normalize(u.name) AS search_name
FROM users u
UNION ALL
SELECT h.user_id AS user_id, search_normalize(h.name) AS search_name
FROM user_name_history h;

CREATE INDEX IF NOT EXISTS idx_users_search_name_trgm
    ON users USING gin (search_normalize(name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_user_name_history_search_name_trgm
    ON user_name_history USING gin (search_normalize(name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_campaigns_search_name_trgm
    ON campaigns USING gin (search_normalize(name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_maps_search_song_name_trgm
    ON maps USING gin (search_normalize(song_name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_maps_search_song_author_trgm
    ON maps USING gin (search_normalize(song_author) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_maps_search_map_author_trgm
    ON maps USING gin (search_normalize(map_author) gin_trgm_ops);

DROP INDEX IF EXISTS idx_users_name_trgm;
DROP INDEX IF EXISTS idx_maps_song_name_trgm;
DROP INDEX IF EXISTS idx_maps_song_author_trgm;
DROP INDEX IF EXISTS idx_maps_map_author_trgm;
DROP INDEX IF EXISTS idx_user_name_history_name;
