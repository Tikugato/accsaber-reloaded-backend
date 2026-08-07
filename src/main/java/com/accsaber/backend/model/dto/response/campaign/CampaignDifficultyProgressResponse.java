package com.accsaber.backend.model.dto.response.campaign;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CampaignDifficultyProgressResponse {

    private CampaignDifficultyResponse node;
    private Double userValue;
    private List<CampaignTargetProgressResponse> targets;
    private Integer userScore;
    private boolean completed;
    private boolean unlocked;
    private boolean pathCompleted;
    private boolean rewardsEarned;
}
