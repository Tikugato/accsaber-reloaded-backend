ALTER TABLE campaigns
    ADD COLUMN loved    BOOLEAN     NOT NULL DEFAULT false,
    ADD COLUMN loved_at TIMESTAMPTZ,
    ADD COLUMN loved_by UUID REFERENCES staff_users(id);

CREATE INDEX idx_campaigns_loved ON campaigns(loved) WHERE loved = true AND active = true;
