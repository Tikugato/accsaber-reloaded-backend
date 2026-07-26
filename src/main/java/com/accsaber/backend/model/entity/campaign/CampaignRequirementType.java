package com.accsaber.backend.model.entity.campaign;

public enum CampaignRequirementType {
    ACC,
    AP,
    SCORE,
    STREAK_115,
    FC,
    RANK,
    PASS,
    COMBO,
    BOMB_HITS;

    public boolean isLowerBetter() {
        return this == RANK;
    }
}
