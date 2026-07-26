package com.accsaber.backend.model.dto.response.campaign;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CampaignTargetProgressResponse {

    private CampaignTargetResponse target;
    private BigDecimal userValue;
    private boolean met;
}
