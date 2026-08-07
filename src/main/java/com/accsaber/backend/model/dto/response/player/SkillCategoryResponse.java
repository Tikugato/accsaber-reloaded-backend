package com.accsaber.backend.model.dto.response.player;


import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SkillCategoryResponse {

    String categoryCode;
    String categoryName;
    double skillLevel;
    SkillComponents components;

    @Value
    @Builder
    public static class SkillComponents {
        double rank;
        double sustained;
        double peak;
        double combined;
        Double rawApForOneGain;
        Double topAp;
        Integer categoryRank;
        Long activePlayers;
    }
}
