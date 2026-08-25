package com.accsaber.backend.model.dto.request.milestone;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import lombok.Data;

@Data
public class CreateMilestoneSetRequest {

    @NotBlank
    private String title;

    private String description;

    private Double setBonusXp;

    @Valid
    private List<MilestoneRewardRequest> rewards;
}
