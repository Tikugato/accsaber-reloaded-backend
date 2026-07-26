package com.accsaber.backend.model.dto.response.campaign;

import java.math.BigDecimal;
import java.util.UUID;

import com.accsaber.backend.model.entity.campaign.CampaignRequirementType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CampaignTargetResponse {

    private UUID id;
    private CampaignRequirementType requirementType;
    private BigDecimal requirementValue;
    private BigDecimal requirementValueMax;
}
