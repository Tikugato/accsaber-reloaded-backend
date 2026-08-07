package com.accsaber.backend.model.dto.response.campaign;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BarrierProgressResponse {

    private CampaignBarrierResponse barrier;
    private Double currentValue;
    private boolean satisfied;
    private boolean unlocked;
}
