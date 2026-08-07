package com.accsaber.backend.model.dto.response.player;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserCategoryStatisticsResponse {

    private UUID id;
    private String userId;
    private UUID categoryId;
    private Integer ranking;
    private Integer countryRanking;
    private Double ap;
    private Double scoreXp;
    private Double averageAcc;
    private Double averageAp;
    private Integer rankedPlays;
    private UUID topPlayId;
    private Instant createdAt;
    private Instant updatedAt;
}
