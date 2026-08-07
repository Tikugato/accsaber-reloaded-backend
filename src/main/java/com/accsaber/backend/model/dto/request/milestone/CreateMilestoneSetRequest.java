package com.accsaber.backend.model.dto.request.milestone;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateMilestoneSetRequest {

    @NotBlank
    private String title;

    private String description;

    private Double setBonusXp;

    private UUID awardsItemId;
}
