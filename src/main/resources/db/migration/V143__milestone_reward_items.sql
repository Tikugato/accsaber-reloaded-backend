CREATE TABLE milestone_items (
    milestone_id UUID        NOT NULL REFERENCES milestones(id),
    item_id      UUID        NOT NULL REFERENCES items(id),
    quantity     INTEGER     NOT NULL DEFAULT 1 CHECK (quantity > 0),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (milestone_id, item_id)
);

CREATE INDEX idx_milestone_items_item ON milestone_items(item_id);

CREATE TABLE milestone_set_items (
    set_id     UUID        NOT NULL REFERENCES milestone_sets(id),
    item_id    UUID        NOT NULL REFERENCES items(id),
    quantity   INTEGER     NOT NULL DEFAULT 1 CHECK (quantity > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (set_id, item_id)
);

CREATE INDEX idx_milestone_set_items_item ON milestone_set_items(item_id);

INSERT INTO milestone_items (milestone_id, item_id, quantity)
SELECT id, awards_item_id, 1
FROM milestones
WHERE awards_item_id IS NOT NULL;

INSERT INTO milestone_set_items (set_id, item_id, quantity)
SELECT id, awards_item_id, 1
FROM milestone_sets
WHERE awards_item_id IS NOT NULL;

DO $$
DECLARE
    milestone_gap INTEGER;
    set_gap INTEGER;
BEGIN
    SELECT COUNT(*) INTO milestone_gap FROM milestones m
    WHERE m.awards_item_id IS NOT NULL
      AND NOT EXISTS (SELECT 1 FROM milestone_items mi WHERE mi.milestone_id = m.id);

    SELECT COUNT(*) INTO set_gap FROM milestone_sets s
    WHERE s.awards_item_id IS NOT NULL
      AND NOT EXISTS (SELECT 1 FROM milestone_set_items si WHERE si.set_id = s.id);

    IF milestone_gap > 0 OR set_gap > 0 THEN
        RAISE EXCEPTION 'Reward migration incomplete: % milestones, % sets unmigrated',
            milestone_gap, set_gap;
    END IF;
END $$;

ALTER TABLE milestones DROP COLUMN awards_item_id;
ALTER TABLE milestone_sets DROP COLUMN awards_item_id;
