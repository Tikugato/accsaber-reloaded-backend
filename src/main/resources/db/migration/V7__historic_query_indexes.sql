CREATE INDEX idx_scores_user_difficulty_created
    ON scores(user_id, map_difficulty_id, created_at);

CREATE INDEX idx_user_cat_stats_user_category_created
    ON user_category_statistics(user_id, category_id, created_at);
