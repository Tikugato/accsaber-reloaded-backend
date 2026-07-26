CREATE VIEW campaign_reward_totals AS
SELECT
    c.id AS campaign_id,
    COALESCE(x.node_xp, 0) + c.completion_xp AS total_xp,
    (COALESCE(ni.item_count, 0) + COALESCE(ci.item_count, 0))::INTEGER AS total_rewards
FROM campaigns c
LEFT JOIN (
    SELECT campaign_id, SUM(xp) AS node_xp
    FROM campaign_difficulties
    WHERE active = true
    GROUP BY campaign_id
) x ON x.campaign_id = c.id
LEFT JOIN (
    SELECT cd.campaign_id, SUM(cdi.quantity) AS item_count
    FROM campaign_difficulty_items cdi
    JOIN campaign_difficulties cd
      ON cd.id = cdi.campaign_difficulty_id AND cd.active = true
    GROUP BY cd.campaign_id
) ni ON ni.campaign_id = c.id
LEFT JOIN (
    SELECT campaign_id, SUM(quantity) AS item_count
    FROM campaign_completion_items
    GROUP BY campaign_id
) ci ON ci.campaign_id = c.id;
