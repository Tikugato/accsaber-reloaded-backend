package com.accsaber.backend.util;

import com.accsaber.backend.util.Rounding;

import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.accsaber.backend.model.dto.projection.UserMapDifficultyBests;
import com.accsaber.backend.model.entity.campaign.BarrierConditionType;
import com.accsaber.backend.model.entity.campaign.CampaignDifficultyTarget;
import com.accsaber.backend.model.entity.campaign.CampaignRequirementType;
import com.accsaber.backend.model.entity.map.MapDifficulty;
import com.accsaber.backend.model.entity.score.Score;

public final class CampaignScoreMetrics {

    private CampaignScoreMetrics() {
    }

    public static Double accuracy(Score score) {
        MapDifficulty md = score.getMapDifficulty();
        if (md == null || md.getMaxScore() == null || md.getMaxScore() == 0 || score.getScoreNoMods() == null) {
            return null;
        }
        return Rounding.round((double) (score.getScoreNoMods()) / (double) (md.getMaxScore()), 6);
    }

    public static boolean isFullCombo(Score score) {
        return score.getMisses() != null && score.getBadCuts() != null
                && score.getMisses() == 0 && score.getBadCuts() == 0;
    }

    public static Integer mistakes(Score score) {
        if (score.getBadCuts() == null || score.getMisses() == null) {
            return null;
        }
        return score.getBadCuts() + score.getMisses();
    }

    public static Double requirementValue(Score score, CampaignRequirementType type, ScoreModifierIndex modifiers) {
        return switch (type) {
            case ACC -> accuracy(score);
            case AP -> score.getAp();
            case SCORE -> score.getScore() != null ? (double) (score.getScore()) : null;
            case STREAK_115 -> score.getStreak115() != null ? (double) (score.getStreak115()) : null;
            case FC -> isFullCombo(score) ? 1.0 : 0.0;
            case RANK -> score.getRank() != null ? (double) (score.getRank()) : null;
            case PASS -> modifiers.hasNoFail(score.getId()) ? 0.0 : 1.0;
            case COMBO -> toDecimal(score.getMaxCombo());
            case BOMB_HITS -> toDecimal(score.getBombHits());
            case MISTAKES -> toDecimal(mistakes(score));
        };
    }

    public static boolean satisfies(CampaignDifficultyTarget target, Double value) {
        CampaignRequirementType type = target.getRequirementType();
        return satisfiesBounds(
                toDisplayPrecision(value, type),
                toDisplayPrecision(target.getRequirementValue(), type),
                toDisplayPrecision(target.getRequirementValueMax(), type),
                type.isLowerBetter());
    }

    public static boolean satisfiesBounds(Double value, Double bound, Double cap,
            boolean lowerBetter) {
        if (value == null || (bound == null && cap == null)) {
            return false;
        }
        if (bound != null) {
            int cmp = value.compareTo(bound);
            if (lowerBetter ? cmp > 0 : cmp < 0) {
                return false;
            }
        }
        return cap == null || value.compareTo(cap) <= 0;
    }

    public static Double bestAccuracy(UserMapDifficultyBests bests) {
        if (bests.maxScore() == null || bests.maxScore() == 0 || bests.bestScoreNoMods() == null) {
            return null;
        }
        return Rounding.round((double) (bests.bestScoreNoMods()) / (double) (bests.maxScore()), 6);
    }

    public static Double requirementValue(UserMapDifficultyBests bests, CampaignRequirementType type) {
        return switch (type) {
            case ACC -> bestAccuracy(bests);
            case AP -> bests.bestAp();
            case SCORE -> toDecimal(bests.bestScore());
            case STREAK_115 -> toDecimal(bests.bestStreak115());
            case FC -> bests.hasFullCombo() ? 1.0 : 0.0;
            case RANK -> toDecimal(bests.bestRank());
            case PASS -> bests.hasNoNfPass() ? 1.0 : 0.0;
            case COMBO -> toDecimal(bests.bestCombo());
            case BOMB_HITS -> toDecimal(bests.fewestBombHits());
            case MISTAKES -> toDecimal(bests.fewestMistakes());
        };
    }

    public static Double barrierMetric(UserMapDifficultyBests bests, BarrierConditionType type) {
        return switch (type) {
            case AVERAGE_ACC, ACC_MAX -> bestAccuracy(bests);
            case AVERAGE_AP, AP_MAX -> bests.bestAp();
            case STREAK_115_AVERAGE, STREAK_115_MAX -> toDecimal(bests.bestStreak115());
            case AVERAGE_RANK, MAX_RANK -> toDecimal(bests.bestRank());
            case AVERAGE_COMBO -> toDecimal(bests.bestCombo());
            case AVERAGE_BOMB_HITS -> toDecimal(bests.fewestBombHits());
            case AVERAGE_MISTAKES -> toDecimal(bests.fewestMistakes());
            case FC, COMPLETION_COUNT, PASS -> null;
        };
    }

    private static Double toDecimal(Integer value) {
        return value != null ? (double) (value) : null;
    }

    public static Double toDisplayPrecision(Double value, CampaignRequirementType type) {
        return type == CampaignRequirementType.ACC ? displayAcc(value) : value;
    }

    public static Double toDisplayPrecision(Double value, BarrierConditionType type) {
        return type == BarrierConditionType.AVERAGE_ACC || type == BarrierConditionType.ACC_MAX
                ? displayAcc(value)
                : value;
    }

    private static Double displayAcc(Double acc) {
        return acc == null ? null : Rounding.round(acc, 4);
    }

    public static boolean isMaxAggregate(BarrierConditionType type) {
        return type == BarrierConditionType.AP_MAX
                || type == BarrierConditionType.ACC_MAX
                || type == BarrierConditionType.STREAK_115_MAX
                || type == BarrierConditionType.MAX_RANK;
    }

    public static Double max(List<Double> values) {
        Double maximum = values.get(0);
        for (Double v : values) {
            if (v.compareTo(maximum) > 0) {
                maximum = v;
            }
        }
        return maximum;
    }

    public static Double average(List<Double> values) {
        double sum = 0.0;
        for (Double v : values) {
            sum += v;
        }
        return Rounding.round(sum / values.size(), 6);
    }

    public static Instant effectiveTime(Score score) {
        return score.getTimeSet() != null ? score.getTimeSet() : score.getCreatedAt();
    }

    public static UserMapDifficultyBests reduceBests(UUID mapDifficultyId, Integer maxScore,
            Collection<Score> rows, ScoreModifierIndex modifiers) {
        if (rows.isEmpty()) {
            return null;
        }
        Integer bestScore = null;
        Integer bestScoreNoMods = null;
        Double bestAp = null;
        Integer bestStreak115 = null;
        Integer bestRank = null;
        Integer bestCombo = null;
        Integer fewestBombHits = null;
        Integer fewestMistakes = null;
        int fcFlag = 0;
        int noNfFlag = 0;
        for (Score s : rows) {
            bestCombo = maxOf(bestCombo, s.getMaxCombo());
            fewestBombHits = minOf(fewestBombHits, s.getBombHits());
            fewestMistakes = minOf(fewestMistakes, mistakes(s));
            bestScore = maxOf(bestScore, s.getScore());
            bestScoreNoMods = maxOf(bestScoreNoMods, s.getScoreNoMods());
            if (bestAp == null || s.getAp() > bestAp) {
                bestAp = s.getAp();
            }
            bestStreak115 = maxOf(bestStreak115, s.getStreak115());
            if (s.isActive() && s.getRank() != null && s.getRankWhenSet() != null) {
                int rank = Math.min(s.getRank(), s.getRankWhenSet());
                if (bestRank == null || rank < bestRank) {
                    bestRank = rank;
                }
            }
            if (isFullCombo(s)) {
                fcFlag = 1;
            }
            if (!modifiers.hasNoFail(s.getId())) {
                noNfFlag = 1;
            }
        }
        return new UserMapDifficultyBests(mapDifficultyId, maxScore, bestScore, bestScoreNoMods,
                bestAp, bestStreak115, bestRank, fcFlag, noNfFlag, bestCombo, fewestBombHits,
                fewestMistakes);
    }

    private static Integer maxOf(Integer current, Integer candidate) {
        if (candidate == null) {
            return current;
        }
        return current == null || candidate > current ? candidate : current;
    }

    private static Integer minOf(Integer current, Integer candidate) {
        if (candidate == null) {
            return current;
        }
        return current == null || candidate < current ? candidate : current;
    }
}
