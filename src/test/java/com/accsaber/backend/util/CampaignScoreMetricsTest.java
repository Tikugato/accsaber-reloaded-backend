package com.accsaber.backend.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.accsaber.backend.model.entity.campaign.CampaignDifficultyTarget;
import com.accsaber.backend.model.entity.campaign.CampaignRequirementType;
import com.accsaber.backend.model.entity.map.MapDifficulty;
import com.accsaber.backend.model.entity.score.Score;

class CampaignScoreMetricsTest {

    private static final int MAX_SCORE = 1_000_000;
    private static final ScoreModifierIndex NO_MODIFIERS = new ScoreModifierIndex(Map.of(), Set.of());

    private MapDifficulty mapDifficulty;

    @BeforeEach
    void setUp() {
        mapDifficulty = MapDifficulty.builder().id(UUID.randomUUID()).maxScore(MAX_SCORE).build();
    }

    private Score run(double accuracy, int streak115) {
        return Score.builder()
                .id(UUID.randomUUID())
                .mapDifficulty(mapDifficulty)
                .score((int) Math.round(accuracy * MAX_SCORE))
                .scoreNoMods((int) Math.round(accuracy * MAX_SCORE))
                .streak115(streak115)
                .rank(1)
                .rankWhenSet(1)
                .active(true)
                .build();
    }

    private static CampaignDifficultyTarget target(CampaignRequirementType type, Double value, Double max) {
        return CampaignDifficultyTarget.builder()
                .requirementType(type)
                .requirementValue(value)
                .requirementValueMax(max)
                .build();
    }

    @Nested
    @DisplayName("bestMatchingRow")
    class BestMatchingRow {

        @Test
        void picksTheRunThatSatisfiesEveryTargetRatherThanTheBestOfEachMetric() {
            List<CampaignDifficultyTarget> targets = List.of(
                    target(CampaignRequirementType.ACC, 0.98, 0.993),
                    target(CampaignRequirementType.STREAK_115, 3.0, null));
            Score overshoot = run(0.9937, 6);
            Score qualifying = run(0.9826, 3);

            Score reference = CampaignScoreMetrics.bestMatchingRow(List.of(overshoot, qualifying), targets, true,
                    NO_MODIFIERS);

            assertThat(reference).isSameAs(qualifying);
            assertThat(CampaignScoreMetrics.satisfies(targets.get(0),
                    CampaignScoreMetrics.rowValue(reference, CampaignRequirementType.ACC, NO_MODIFIERS))).isTrue();
            assertThat(CampaignScoreMetrics.satisfies(targets.get(1), CampaignScoreMetrics
                    .rowValue(reference, CampaignRequirementType.STREAK_115, NO_MODIFIERS))).isTrue();
        }

        @Test
        void picksTheRunClosestToTheRangeWhenNoneQualify() {
            List<CampaignDifficultyTarget> targets = List.of(target(CampaignRequirementType.ACC, 0.98, 0.99));
            Score farAbove = run(0.9990, 8);
            Score justAbove = run(0.9910, 2);
            Score farBelow = run(0.9000, 1);

            Score reference = CampaignScoreMetrics.bestMatchingRow(List.of(farAbove, justAbove, farBelow), targets,
                    true, NO_MODIFIERS);

            assertThat(reference).isSameAs(justAbove);
        }

        @Test
        void picksTheHighestScoringRunAmongQualifyingRuns() {
            List<CampaignDifficultyTarget> targets = List.of(target(CampaignRequirementType.ACC, 0.95, null));
            Score lower = run(0.96, 1);
            Score higher = run(0.98, 1);

            Score reference = CampaignScoreMetrics.bestMatchingRow(List.of(lower, higher), targets, true,
                    NO_MODIFIERS);

            assertThat(reference).isSameAs(higher);
        }

        @Test
        void satisfiesAnySingleTargetInOrMode() {
            List<CampaignDifficultyTarget> targets = List.of(
                    target(CampaignRequirementType.ACC, 0.99, null),
                    target(CampaignRequirementType.STREAK_115, 10.0, null));
            Score streakRun = run(0.94, 12);

            Score reference = CampaignScoreMetrics.bestMatchingRow(List.of(streakRun), targets, false, NO_MODIFIERS);

            assertThat(CampaignScoreMetrics.targetsMet(targets, false,
                    type -> CampaignScoreMetrics.rowValue(reference, type, NO_MODIFIERS))).isTrue();
        }

        @Test
        void returnsNullWhenThereAreNoRuns() {
            List<CampaignDifficultyTarget> targets = List.of(target(CampaignRequirementType.ACC, 0.95, null));

            assertThat(CampaignScoreMetrics.bestMatchingRow(List.of(), targets, true, NO_MODIFIERS)).isNull();
        }
    }
}
