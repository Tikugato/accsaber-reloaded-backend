package com.accsaber.backend.model.dto.projection;

import java.util.UUID;

public record UserMapDifficultyBests(
        UUID mapDifficultyId,
        Integer maxScore,
        Integer bestScore,
        Integer bestScoreNoMods,
        Double bestAp,
        Integer bestStreak115,
        Integer bestRank,
        Integer fcFlag,
        Integer noNfFlag,
        Integer bestCombo,
        Integer fewestBombHits,
        Integer fewestMistakes) {

    public boolean hasFullCombo() {
        return fcFlag != null && fcFlag == 1;
    }

    public boolean hasNoNfPass() {
        return noNfFlag != null && noNfFlag == 1;
    }
}
