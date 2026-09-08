package com.accsaber.backend.model.dto.response.statistics;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CampaignCreatorResponse {

    private String userId;
    private String userName;
    private String avatarUrl;
    private String cdnAvatarUrl;
    private String country;
    private long campaigns;
    private long curatedCampaigns;
    private long participants;
    private long completions;
    private Double completionRate;
}
