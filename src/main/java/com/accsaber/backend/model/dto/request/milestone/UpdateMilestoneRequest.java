package com.accsaber.backend.model.dto.request.milestone;

import java.util.List;
import java.util.UUID;

import com.accsaber.backend.model.dto.MilestoneQuerySpec;
import com.accsaber.backend.model.entity.milestone.MilestoneProgressModel;
import com.accsaber.backend.model.entity.milestone.MilestoneTier;

import jakarta.validation.Valid;
import lombok.Data;

@Data
public class UpdateMilestoneRequest {

    private String title;

    private String description;

    private MilestoneQuerySpec querySpec;

    private Double xp;

    private MilestoneTier tier;



    private String iconGroup;
    private Double targetValue;

    private String comparison;

    @Valid
    private List<MilestoneRewardRequest> rewards;

    private Double positionX;

    private Double positionY;

    private MilestoneProgressModel progressModel;

    private UUID progressCurveId;

    private Double progressFloor;
}
