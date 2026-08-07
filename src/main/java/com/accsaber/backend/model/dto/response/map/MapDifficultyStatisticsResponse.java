package com.accsaber.backend.model.dto.response.map;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MapDifficultyStatisticsResponse {

    UUID id;
    Double maxAp;
    Double minAp;
    Double averageAp;
    int totalScores;
    TopScoreSnapshot topScore;
    Instant createdAt;
}
