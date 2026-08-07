package com.accsaber.backend.model.dto;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventMissionTargets(
        UUID categoryId,
        UUID mapDifficultyId,
        String playerId,
        Double acc,
        Double ap,
        Integer score,
        Integer count,
        Integer xp,
        Double thresholdAp,
        Integer streak,
        Instant rankedBefore,
        Boolean curatedOnly,
        Boolean requirePass) {

    public Long playerIdAsLong() {
        return playerId == null ? null : Long.valueOf(playerId);
    }
}
