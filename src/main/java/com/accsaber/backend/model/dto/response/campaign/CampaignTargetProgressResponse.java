package com.accsaber.backend.model.dto.response.campaign;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CampaignTargetProgressResponse {

    private CampaignTargetResponse target;
    private Double userValue;
    private boolean met;
}
