package com.accsaber.backend.model.dto.response.campaign;

import com.accsaber.backend.model.dto.response.ModifierResponse;
import com.accsaber.backend.model.entity.campaign.CampaignModifierRequirement;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CampaignModifierRequirementResponse {

    private ModifierResponse modifier;
    private CampaignModifierRequirement requirement;
}
