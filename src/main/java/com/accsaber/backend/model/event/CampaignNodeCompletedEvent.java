package com.accsaber.backend.model.event;

import java.time.Instant;
import java.util.UUID;

public record CampaignNodeCompletedEvent(Long userId, UUID campaignId, UUID nodeId, Instant completedAt,
        boolean silent) {

    public CampaignNodeCompletedEvent(Long userId, UUID campaignId, UUID nodeId, Instant completedAt) {
        this(userId, campaignId, nodeId, completedAt, false);
    }
}
