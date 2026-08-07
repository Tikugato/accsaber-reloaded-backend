package com.accsaber.backend.model.dto.request.campaign;


import com.accsaber.backend.model.entity.campaign.CampaignRequirementType;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CampaignTargetRequest {

    @NotNull
    private CampaignRequirementType requirementType;

    private Double requirementValue;

    private Double requirementValueMax;
}
