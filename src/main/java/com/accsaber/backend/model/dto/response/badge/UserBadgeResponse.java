package com.accsaber.backend.model.dto.response.badge;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserBadgeResponse {

    UUID id;
    UUID badgeId;
    String badgeName;
    String badgeDescription;
    String badgeImageUrl;
    UUID awardedByStaffId;
    String reason;
    Instant awardedAt;
}
