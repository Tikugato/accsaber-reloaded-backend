package com.accsaber.backend.model.dto.request.badge;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AwardBadgeRequest {

    @NotNull
    private Long userId;

    @NotNull
    private UUID badgeId;

    private String reason;
}
