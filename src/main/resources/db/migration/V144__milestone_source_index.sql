CREATE INDEX idx_milestones_query_source
    ON milestones ((query_spec ->> 'from'))
    WHERE active = true AND status = 'ACTIVE';
