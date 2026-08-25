package com.accsaber.backend.model.dto.request.milestone;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class MilestoneRewardRequest {

    @NotNull
    private UUID itemId;

    @NotNull
    @Positive
    private Integer quantity = 1;
}
