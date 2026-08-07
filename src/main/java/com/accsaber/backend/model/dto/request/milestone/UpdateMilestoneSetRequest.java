package com.accsaber.backend.model.dto.request.milestone;

import java.util.UUID;

import lombok.Data;

@Data
public class UpdateMilestoneSetRequest {

    private String title;

    private String description;

    private Double setBonusXp;

    private UUID awardsItemId;
}
