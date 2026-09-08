package com.accsaber.backend.model.dto.response.statistics;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventMissionLeaderboardResponse {

    private long rank;
    private String userId;
    private String userName;
    private String avatarUrl;
    private String cdnAvatarUrl;
    private String country;
    private long completions;
    private long xpEarned;
    private long itemsAwarded;
    private Instant lastCompletedAt;
}
