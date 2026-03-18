ALTER TABLE milestone_sets
    ADD COLUMN awards_badge_id UUID REFERENCES badges(id);
