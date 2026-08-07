package com.accsaber.backend.model.dto.response.score;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyScoreSummary {

    private UUID id;
    private Integer score;
    private Double accuracy;
    private Double ap;
    private Double weightedAp;
    private Integer rank;
    private Instant timeSet;
}
