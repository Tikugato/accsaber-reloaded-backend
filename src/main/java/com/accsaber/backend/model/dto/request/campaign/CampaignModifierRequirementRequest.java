package com.accsaber.backend.model.dto.request.campaign;

import java.util.UUID;

import com.accsaber.backend.model.entity.campaign.CampaignModifierRequirement;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CampaignModifierRequirementRequest {

    @NotNull
    private UUID modifierId;

    @NotNull
    private CampaignModifierRequirement requirement;
}
