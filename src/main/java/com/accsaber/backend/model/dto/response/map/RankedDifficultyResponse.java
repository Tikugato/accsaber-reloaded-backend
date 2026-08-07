package com.accsaber.backend.model.dto.response.map;

import java.time.Instant;
import java.util.UUID;

import com.accsaber.backend.model.entity.map.Difficulty;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RankedDifficultyResponse {
    UUID id;
    String songHash;
    String songName;
    Difficulty difficulty;
    Double complexity;
    String categoryCode;
    String ssLeaderboardId;
    String blLeaderboardId;
    Instant rankedAt;
}
