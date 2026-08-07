DROP VIEW IF EXISTS campaign_reward_totals;
DROP MATERIALIZED VIEW IF EXISTS milestone_completion_stats;

DO $$
DECLARE
    r RECORD;
    converted INTEGER := 0;
BEGIN
    FOR r IN
        SELECT c.table_name, c.column_name
        FROM information_schema.columns c
        JOIN information_schema.tables t
          ON t.table_schema = c.table_schema
         AND t.table_name = c.table_name
        WHERE c.table_schema = 'public'
          AND c.data_type = 'numeric'
          AND t.table_type = 'BASE TABLE'
        ORDER BY c.table_name, c.column_name
    LOOP
        EXECUTE format(
            'ALTER TABLE public.%I ALTER COLUMN %I TYPE DOUBLE PRECISION USING %I::DOUBLE PRECISION',
            r.table_name, r.column_name, r.column_name);
        converted := converted + 1;
    END LOOP;

    RAISE NOTICE 'V136 converted % numeric columns to double precision', converted;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns c
        JOIN information_schema.tables t
          ON t.table_schema = c.table_schema
         AND t.table_name = c.table_name
        WHERE c.table_schema = 'public'
          AND c.data_type = 'numeric'
          AND t.table_type = 'BASE TABLE'
    ) THEN
        RAISE EXCEPTION 'V136 left numeric columns behind';
    END IF;
END $$;

CREATE MATERIALIZED VIEW milestone_completion_stats AS
SELECT
    m.id AS milestone_id,
    COUNT(uml.id) FILTER (WHERE uml.completed = true) AS completions,
    (SELECT COUNT(*) FROM users WHERE active = true AND banned = false) AS total_players,
    CASE WHEN (SELECT COUNT(*) FROM users WHERE active = true AND banned = false) = 0 THEN 0::DOUBLE PRECISION
         ELSE ROUND(COUNT(uml.id) FILTER (WHERE uml.completed = true) * 100.0 /
              (SELECT COUNT(*) FROM users WHERE active = true AND banned = false), 6)::DOUBLE PRECISION
    END AS completion_percentage
FROM milestones m
LEFT JOIN user_milestone_links uml ON uml.milestone_id = m.id
WHERE m.active = true
GROUP BY m.id;

CREATE UNIQUE INDEX idx_milestone_completion_stats_id ON milestone_completion_stats(milestone_id);

CREATE VIEW campaign_reward_totals AS
SELECT
    c.id AS campaign_id,
    (COALESCE(x.node_xp, 0) + c.completion_xp)::DOUBLE PRECISION AS total_xp,
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
