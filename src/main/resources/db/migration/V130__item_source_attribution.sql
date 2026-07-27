ALTER TABLE user_item_links DROP CONSTRAINT user_item_links_source_check;
ALTER TABLE user_item_links ADD CONSTRAINT user_item_links_source_check
    CHECK (source IN ('milestone', 'milestone_set', 'campaign_milestone', 'campaign_difficulty',
                      'campaign_completion', 'level', 'mission', 'event', 'welcome', 'trade',
                      'manual', 'system', 'crate_drop', 'supporter_tier', 'market'));

UPDATE user_item_links
SET source    = 'mission',
    source_id = substring(source_id FROM 9)
WHERE source = 'manual'
  AND source_id LIKE 'mission:%';

UPDATE user_item_links
SET source    = 'event',
    source_id = substring(source_id FROM 7)
WHERE source = 'manual'
  AND source_id LIKE 'event:%';

UPDATE user_item_links
SET source    = 'welcome',
    source_id = NULL
WHERE source = 'manual'
  AND (source_id = 'welcome'
       OR (awarded_by IS NULL
           AND item_id IN (SELECT id FROM items WHERE is_welcome_grant)));

UPDATE user_item_links
SET source = 'system'
WHERE source = 'manual'
  AND awarded_by IS NULL;

UPDATE user_item_links
SET awarded_by = NULL
WHERE source <> 'manual'
  AND awarded_by IS NOT NULL;

ALTER TABLE user_item_links ADD CONSTRAINT user_item_links_manual_is_staff_awarded
    CHECK ((source = 'manual') = (awarded_by IS NOT NULL));
