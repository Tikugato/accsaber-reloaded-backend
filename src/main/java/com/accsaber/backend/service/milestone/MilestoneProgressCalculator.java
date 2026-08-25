package com.accsaber.backend.service.milestone;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.accsaber.backend.model.entity.Category;
import com.accsaber.backend.model.entity.Curve;
import com.accsaber.backend.model.entity.milestone.Milestone;
import com.accsaber.backend.repository.CategoryRepository;
import com.accsaber.backend.repository.user.UserCategoryStatisticsRepository;
import com.accsaber.backend.service.score.APCalculationService;
import com.accsaber.backend.util.Rounding;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MilestoneProgressCalculator {

    private static final String OVERALL_CODE = "overall";
    private static final UUID GLOBAL_POPULATION = new UUID(0L, 0L);
    private static final int SCALE = 6;

    private final APCalculationService apCalculationService;
    private final CategoryRepository categoryRepository;
    private final UserCategoryStatisticsRepository userCategoryStatisticsRepository;

    private final Map<UUID, Long> populationCache = new ConcurrentHashMap<>();

    public Double normalize(Milestone milestone, Double progress) {
        if (progress == null || milestone.getTargetValue() == null) {
            return null;
        }
        double value = progress;
        double target = milestone.getTargetValue();
        boolean lowerIsBetter = "LTE".equals(milestone.getComparison());

        Double fraction = switch (milestone.getProgressModel()) {
            case CURVE -> curveFraction(milestone, value, target, lowerIsBetter);
            case LOG -> logFraction(milestone, value, target, lowerIsBetter);
            case LINEAR -> linearFraction(milestone, value, target, lowerIsBetter);
        };
        return fraction == null ? null : clamp(fraction);
    }

    public void evictPopulationCache() {
        populationCache.clear();
    }

    private Double linearFraction(Milestone milestone, double value, double target, boolean lowerIsBetter) {
        Double floor = milestone.getProgressFloor();
        if (floor != null) {
            return anchored(value, target, floor);
        }
        if (lowerIsBetter) {
            return value <= 0 ? null : target / value;
        }
        return target == 0 ? 1.0 : value / target;
    }

    private Double curveFraction(Milestone milestone, double value, double target, boolean lowerIsBetter) {
        Curve curve = milestone.getProgressCurve();
        if (curve == null) {
            log.warn("Milestone {} uses CURVE progress with no curve attached", milestone.getId());
            return linearFraction(milestone, value, target, lowerIsBetter);
        }
        double mappedValue = apCalculationService.interpolate(curve.getId(), value);
        double mappedTarget = apCalculationService.interpolate(curve.getId(), target);
        Double floor = milestone.getProgressFloor();
        if (floor != null) {
            return anchored(mappedValue, mappedTarget, apCalculationService.interpolate(curve.getId(), floor));
        }
        if (lowerIsBetter) {
            return mappedValue <= 0 ? null : mappedTarget / mappedValue;
        }
        return mappedTarget == 0 ? 1.0 : mappedValue / mappedTarget;
    }

    private Double logFraction(Milestone milestone, double value, double target, boolean lowerIsBetter) {
        if (!lowerIsBetter) {
            Double floor = milestone.getProgressFloor();
            if (floor == null || floor <= 0 || target <= floor) {
                return null;
            }
            return value <= floor ? 0.0 : Math.log(value / floor) / Math.log(target / floor);
        }
        Double floor = milestone.getProgressFloor() != null
                ? milestone.getProgressFloor()
                : population(milestone);
        if (floor == null || value <= 0 || target <= 0 || floor <= target) {
            return null;
        }
        return value >= floor ? 0.0 : Math.log(floor / value) / Math.log(floor / target);
    }

    private Double anchored(double value, double target, double floor) {
        double span = target - floor;
        if (span == 0) {
            return 1.0;
        }
        return (value - floor) / span;
    }

    private Double population(Milestone milestone) {
        UUID key = milestone.getCategory() != null ? milestone.getCategory().getId() : GLOBAL_POPULATION;
        long count = populationCache.computeIfAbsent(key, this::countPlayers);
        return count <= 1 ? null : (double) count;
    }

    private long countPlayers(UUID key) {
        UUID categoryId = GLOBAL_POPULATION.equals(key)
                ? categoryRepository.findByCodeAndActiveTrue(OVERALL_CODE).map(Category::getId).orElse(null)
                : key;
        return categoryId == null ? 0L : userCategoryStatisticsRepository.countActivePlayersInCategory(categoryId);
    }

    private double clamp(double fraction) {
        return Math.clamp(Rounding.round(fraction, SCALE), 0.0, 1.0);
    }
}
