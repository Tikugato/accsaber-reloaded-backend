package com.accsaber.backend.model.dto.response.milestone;

import com.accsaber.backend.model.dto.response.item.ItemResponse;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MilestoneRewardResponse {

    private ItemResponse item;
    private Integer quantity;
}
