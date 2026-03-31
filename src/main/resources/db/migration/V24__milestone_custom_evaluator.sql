ALTER TABLE milestones ADD COLUMN custom_evaluator TEXT;
ALTER TABLE milestones ALTER COLUMN query_spec DROP NOT NULL;
ALTER TABLE milestones ALTER COLUMN target_value DROP NOT NULL;
