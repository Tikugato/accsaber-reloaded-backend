package com.accsaber.backend.model.dto.response.statistics;

import java.time.Instant;
import java.util.UUID;

import com.accsaber.backend.model.entity.map.Difficulty;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MapAvgApResponse {

    private UUID mapDifficultyId;
    private UUID mapId;
    private String songName;
    private String songAuthor;
    private String mapAuthor;
    private String coverUrl;
    private String cdnCoverUrl;
    private Difficulty difficulty;
    private UUID categoryId;
    private String categoryName;
    private Double averageWeightedAp;
    private long scoreCount;
    private UUID latestScoreId;
    private Instant latestScoreTimeSet;
}
