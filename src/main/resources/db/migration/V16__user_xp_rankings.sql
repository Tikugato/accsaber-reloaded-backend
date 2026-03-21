ALTER TABLE users ADD COLUMN xp_ranking INTEGER;
ALTER TABLE users ADD COLUMN xp_country_ranking INTEGER;

CREATE INDEX idx_users_xp_ranking ON users (xp_ranking) WHERE active = true AND xp_ranking IS NOT NULL;
CREATE INDEX idx_users_xp_country_ranking ON users (country, xp_country_ranking) WHERE active = true AND xp_country_ranking IS NOT NULL;
CREATE INDEX idx_users_total_xp_desc ON users (total_xp DESC) WHERE active = true;
