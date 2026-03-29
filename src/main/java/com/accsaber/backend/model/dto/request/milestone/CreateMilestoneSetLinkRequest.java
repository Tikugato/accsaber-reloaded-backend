package com.accsaber.backend.model.dto.request.milestone;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateMilestoneSetLinkRequest {

    @NotNull
    private UUID groupId;

    @NotNull
    private UUID setId;

    private int sortOrder = 0;
}
