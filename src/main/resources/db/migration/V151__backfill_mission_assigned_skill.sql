UPDATE user_missions m
SET assigned_skill_level     = s.skill_level,
    assigned_skill_threshold = s.raw_ap_for_one_gain
FROM user_category_skills s
WHERE s.user_id = m.user_id
  AND s.category_id = m.category_id
  AND m.user_id IS NOT NULL
  AND m.assigned_skill_level IS NULL
  AND s.raw_ap_for_one_gain IS NOT NULL;

WITH aggregated AS (
    SELECT s.user_id,
           AVG(s.raw_ap_for_one_gain) FILTER (WHERE c.code <> 'overall') AS mean_threshold,
           MAX(s.skill_level) FILTER (WHERE c.code = 'overall')          AS overall_skill
    FROM user_category_skills s
    JOIN categories c ON c.id = s.category_id
    GROUP BY s.user_id
)
UPDATE user_missions m
SET assigned_skill_level     = COALESCE(m.assigned_skill_level, a.overall_skill),
    assigned_skill_threshold = a.mean_threshold
FROM aggregated a
WHERE a.user_id = m.user_id
  AND m.user_id IS NOT NULL
  AND m.assigned_skill_threshold IS NULL
  AND a.mean_threshold IS NOT NULL;
