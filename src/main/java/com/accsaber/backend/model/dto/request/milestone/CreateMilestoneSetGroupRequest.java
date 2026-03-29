package com.accsaber.backend.model.dto.request.milestone;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateMilestoneSetGroupRequest {

    @NotBlank
    private String name;

    private String description;
}
