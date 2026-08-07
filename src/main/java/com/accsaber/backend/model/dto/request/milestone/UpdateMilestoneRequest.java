package com.accsaber.backend.model.dto.request.milestone;

import java.util.UUID;

import com.accsaber.backend.model.dto.MilestoneQuerySpec;
import com.accsaber.backend.model.entity.milestone.MilestoneTier;

import lombok.Data;

@Data
public class UpdateMilestoneRequest {

    private String title;

    private String description;

    private MilestoneQuerySpec querySpec;

    private Double xp;

    private MilestoneTier tier;

    private Double targetValue;

    private String comparison;

    private UUID awardsItemId;
}
