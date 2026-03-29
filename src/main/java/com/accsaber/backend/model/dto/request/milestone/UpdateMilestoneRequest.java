package com.accsaber.backend.model.dto.request.milestone;

import java.math.BigDecimal;

import com.accsaber.backend.model.dto.MilestoneQuerySpec;
import com.accsaber.backend.model.entity.milestone.MilestoneTier;

import lombok.Data;

@Data
public class UpdateMilestoneRequest {

    private String title;

    private String description;

    private MilestoneQuerySpec querySpec;

    private BigDecimal xp;

    private MilestoneTier tier;

    private BigDecimal targetValue;

    private String comparison;
}
