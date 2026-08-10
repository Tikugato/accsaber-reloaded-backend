package com.accsaber.backend.model.dto.response.supporter;

import com.accsaber.backend.model.entity.supporter.SupporterTier;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SupporterTierResponse {

    String tierKey;
    String displayName;
    Integer monthlyCostCents;
    Integer sortOrder;

    public static SupporterTierResponse from(SupporterTier tier) {
        return SupporterTierResponse.builder()
                .tierKey(tier.getTierKey())
                .displayName(tier.getDisplayName())
                .monthlyCostCents(tier.getMonthlyCostCents())
                .sortOrder(tier.getSortOrder())
                .build();
    }
}
