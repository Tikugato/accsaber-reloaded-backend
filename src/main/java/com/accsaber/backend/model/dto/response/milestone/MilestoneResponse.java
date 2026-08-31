package com.accsaber.backend.model.dto.response.milestone;

import java.time.Instant;
import java.util.List;
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

    private String iconGroup;
    private Double xp;
    private MilestoneQuerySpec querySpec;
    private Double targetValue;
    private String comparison;
    private String status;
    private Double completionPercentage;
    private Long completions;
    private Long totalPlayers;
    private List<MilestoneRewardResponse> rewards;
    private double positionX;
    private double positionY;
    private String progressModel;
    private UUID progressCurveId;
    private Double progressFloor;
    private Instant createdAt;
}
