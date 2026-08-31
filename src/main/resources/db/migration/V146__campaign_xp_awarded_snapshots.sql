ALTER TABLE user_campaign_scores
    ADD COLUMN xp_awarded DOUBLE PRECISION NOT NULL DEFAULT 0;

UPDATE user_campaign_scores ucs
SET xp_awarded = cd.xp
FROM campaign_difficulties cd
WHERE ucs.campaign_difficulty_id = cd.id
  AND ucs.rewards_paid = true;

ALTER TABLE user_campaigns
    ADD COLUMN completion_xp_awarded DOUBLE PRECISION NOT NULL DEFAULT 0;

UPDATE user_campaigns uc
SET completion_xp_awarded = c.completion_xp
FROM campaigns c
WHERE uc.campaign_id = c.id
  AND uc.completion_rewards_paid = true;
