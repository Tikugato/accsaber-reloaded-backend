package com.accsaber.backend.model.dto.response.milestone;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MilestoneSetResponse {

    private UUID id;
    private String title;
    private String description;
    private Double setBonusXp;
    private List<MilestoneRewardResponse> rewards;
    private Instant createdAt;
    private Double userCompletionPercentage;
}
