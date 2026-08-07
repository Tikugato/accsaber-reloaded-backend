package com.accsaber.backend.model.dto.response.milestone;

import java.time.Instant;
import java.util.UUID;

import com.accsaber.backend.model.dto.MilestoneQuerySpec;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MilestoneResponse {

    private UUID id;
    private UUID setId;
    private UUID categoryId;
    private String title;
    private String description;
    private String type;
    private String tier;
    private Double xp;
    private MilestoneQuerySpec querySpec;
    private Double targetValue;
    private String comparison;
    private boolean blExclusive;
    private String status;
    private Double completionPercentage;
    private Long completions;
    private Long totalPlayers;
    private UUID awardsItemId;
    private Instant createdAt;
}
