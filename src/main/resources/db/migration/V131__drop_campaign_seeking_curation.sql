DROP INDEX IF EXISTS idx_campaigns_seeking_curation;

ALTER TABLE campaigns
    DROP COLUMN IF EXISTS seeking_curation,
    DROP COLUMN IF EXISTS submitted_at;
