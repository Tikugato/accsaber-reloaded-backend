package com.accsaber.backend.service.mission;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.accsaber.backend.model.entity.Category;
import com.accsaber.backend.model.entity.mission.MissionBand;
import com.accsaber.backend.model.entity.user.UserCategorySkill;

public record MissionAssignmentContext(
        Long userId,
        List<Category> activeCategories,
        Map<UUID, UserCategorySkill> skillByCategoryId,
        Map<UUID, Long> rankedPlaysByCategoryId,
        Double rollingDailyXp,
        Map<ComplexityBandKey, Optional<Double>> normalizedApByComplexityBand,
        Map<StreakBandKey, Integer> streakAbilityByComplexityBand) {

    public MissionAssignmentContext(Long userId, List<Category> activeCategories,
            Map<UUID, UserCategorySkill> skillByCategoryId, Map<UUID, Long> rankedPlaysByCategoryId,
            Double rollingDailyXp) {
        this(userId, activeCategories, skillByCategoryId, rankedPlaysByCategoryId, rollingDailyXp,
                new HashMap<>(), new HashMap<>());
    }

    public record ComplexityBandKey(UUID categoryId, int bandIndex) {
    }

    public record StreakBandKey(UUID categoryId, int bandIndex, MissionBand band) {
    }
}
