ALTER TABLE campaign_difficulties
    ADD COLUMN node_border_url TEXT,
    ADD COLUMN node_border_layer TEXT NOT NULL DEFAULT 'ABOVE'
        CHECK (node_border_layer IN ('ABOVE', 'BELOW'));
