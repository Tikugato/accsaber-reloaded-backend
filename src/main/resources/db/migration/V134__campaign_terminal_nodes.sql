ALTER TABLE campaign_difficulties
    ADD COLUMN terminal BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX idx_campaign_difficulties_terminal
    ON campaign_difficulties(campaign_id)
    WHERE terminal = true AND active = true;

UPDATE campaign_difficulties cd
SET terminal = true
WHERE cd.active = true
  AND cd.barrier = false
  AND EXISTS (SELECT 1
              FROM campaigns c
              WHERE c.id = cd.campaign_id
                AND c.status <> 'draft'
                AND c.completion_mode = 'terminal')
  AND NOT EXISTS (SELECT 1
                  FROM campaign_difficulty_paths p
                  WHERE p.active = true
                    AND p.comes_from_campaign_difficulty_id = cd.id)
  AND NOT EXISTS (SELECT 1
                  FROM campaign_barrier_affected_difficulties a
                  WHERE a.campaign_difficulty_id = cd.id);

UPDATE campaign_difficulties cd
SET terminal = true
WHERE cd.active = true
  AND cd.barrier = false
  AND NOT EXISTS (SELECT 1
                  FROM campaign_difficulty_paths p
                  WHERE p.active = true
                    AND p.comes_from_campaign_difficulty_id = cd.id)
  AND EXISTS (SELECT 1
              FROM campaigns c
              WHERE c.id = cd.campaign_id
                AND c.status <> 'draft'
                AND c.completion_mode = 'terminal'
                AND NOT EXISTS (SELECT 1
                                FROM campaign_difficulties t
                                WHERE t.campaign_id = c.id
                                  AND t.active = true
                                  AND t.terminal = true));

DO $$
DECLARE
    stranded BIGINT;
BEGIN
    SELECT count(*) INTO stranded
    FROM campaigns c
    WHERE c.active = true
      AND c.status <> 'draft'
      AND c.completion_mode = 'terminal'
      AND EXISTS (SELECT 1 FROM campaign_difficulties n
                  WHERE n.campaign_id = c.id AND n.active = true AND n.barrier = false)
      AND NOT EXISTS (SELECT 1 FROM campaign_difficulties t
                      WHERE t.campaign_id = c.id AND t.active = true AND t.terminal = true);

    IF stranded <> 0 THEN
        RAISE EXCEPTION
            'campaign terminal backfill incomplete: % live campaigns have no terminal node',
            stranded;
    END IF;
END $$;
