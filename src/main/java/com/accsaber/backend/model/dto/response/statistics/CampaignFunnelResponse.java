package com.accsaber.backend.model.dto.response.statistics;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CampaignFunnelResponse {

    private UUID campaignId;
    private String name;
    private String slug;
    private String iconUrl;
    private String status;
    private boolean official;
    private boolean loved;
    private long participants;
    private long inProgress;
    private long completed;
    private long abandoned;
    private Double completionRate;
    private Double abandonRate;
    private Double medianDaysToComplete;
    private long nodeCount;
}
