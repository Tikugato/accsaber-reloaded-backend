package com.accsaber.backend.service.score;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.accsaber.backend.model.dto.APResult;
import com.accsaber.backend.model.entity.Curve;
import com.accsaber.backend.model.entity.CurvePoint;
import com.accsaber.backend.model.entity.CurveType;
import com.accsaber.backend.repository.CurvePointRepository;

@ExtendWith(MockitoExtension.class)
class APCalculationServiceTest {

        @Mock
        private CurvePointRepository curvePointRepository;

        @InjectMocks
        private APCalculationService apCalculationService;

        private Curve scoreCurve;
        private Curve weightCurve;

        @BeforeEach
        void setUp() {
                apCalculationService.evictAllCurveCaches();

                scoreCurve = Curve.builder()
                                .id(UUID.randomUUID())
                                .name("Test Score Curve")
                                .type(CurveType.POINT_LOOKUP)
                                .scale(61.0)
                                .shift(-18.0)
                                .build();

                weightCurve = Curve.builder()
                                .id(UUID.randomUUID())
                                .name("Test Weight Curve")
                                .type(CurveType.FORMULA)
                                .formula("LOGISTIC_SIGMOID")
                                .xParameterName("k")
                                .xParameterValue(0.4)
                                .yParameterName("y1")
                                .yParameterValue(0.1)
                                .zParameterName("x1")
                                .zParameterValue(15.0)
                                .build();
        }

        private CurvePoint point(Double x, Double y) {
                return CurvePoint.builder()
                                .id(UUID.randomUUID())
                                .curve(scoreCurve)
                                .x(x)
                                .y(y)
                                .build();
        }

        @Nested
        class Interpolation {

                @Test
                void exactPointHit_returnsExactValue() {
                        List<CurvePoint> points = List.of(
                                        point(0.0, 0.0),
                                        point(0.5, 0.3),
                                        point(1.0, 1.0));
                        when(curvePointRepository.findByCurveIdOrderByXAsc(scoreCurve.getId()))
                                        .thenReturn(points);

                        Double result = apCalculationService.interpolate(scoreCurve, 0.5);

                        assertThat(result).isEqualByComparingTo(0.3);
                }

                @Test
                void interpolatesBetweenPoints() {
                        List<CurvePoint> points = List.of(
                                        point(0.0, 0.0),
                                        point(1.0, 1.0));
                        when(curvePointRepository.findByCurveIdOrderByXAsc(scoreCurve.getId()))
                                        .thenReturn(points);

                        Double result = apCalculationService.interpolate(scoreCurve, 0.25);

                        assertThat(result).isCloseTo(0.25, within(0.0001));
                }

                @Test
                void interpolatesBetweenNonLinearPoints() {
                        List<CurvePoint> points = List.of(
                                        point(0.90, 0.40),
                                        point(0.95, 0.60));
                        when(curvePointRepository.findByCurveIdOrderByXAsc(scoreCurve.getId()))
                                        .thenReturn(points);

                        // Midpoint between 0.90 and 0.95 should give midpoint between 0.40 and 0.60
                        Double result = apCalculationService.interpolate(scoreCurve, 0.925);

                        assertThat(result).isCloseTo(0.50, within(0.0001));
                }

                @Test
                void aboveHighestPoint_returnsHighestValue() {
                        List<CurvePoint> points = List.of(
                                        point(0.0, 0.0),
                                        point(0.99, 0.95));
                        when(curvePointRepository.findByCurveIdOrderByXAsc(scoreCurve.getId()))
                                        .thenReturn(points);

                        Double result = apCalculationService.interpolate(scoreCurve, 1.0);

                        assertThat(result).isEqualByComparingTo(0.95);
                }

                @Test
                void emptyPoints_throwsIllegalState() {
                        when(curvePointRepository.findByCurveIdOrderByXAsc(scoreCurve.getId()))
                                        .thenReturn(Collections.emptyList());

                        assertThatThrownBy(() -> apCalculationService.interpolate(scoreCurve, 0.0))
                                        .isInstanceOf(IllegalStateException.class)
                                        .hasMessageContaining("No curve points loaded");
                }

                @Test
                void formulaCurve_throwsIllegalArgument() {
                        assertThatThrownBy(() -> apCalculationService.interpolate(weightCurve, 0.0))
                                        .isInstanceOf(IllegalArgumentException.class)
                                        .hasMessageContaining("Cannot interpolate a FORMULA type curve");
                }
        }

        @Nested
        class RawAPCalculation {

                @Test
                void calculatesRawAP_withComplexityScaling() {
                        List<CurvePoint> points = List.of(
                                        point(0.0, 0.0),
                                        point(0.95, 0.60),
                                        point(1.0, 1.0));
                        when(curvePointRepository.findByCurveIdOrderByXAsc(scoreCurve.getId()))
                                        .thenReturn(points);

                        Double complexity = 10.5;
                        Double accuracy = 0.95;

                        APResult result = apCalculationService.calculateRawAP(accuracy, complexity, scoreCurve);

                        // curveMultiplier=0.60, complexity=10.5, shift=-18, scale=61
                        // 0.60 * (10.5 - (-18)) * 61 = 0.60 * 28.5 * 61 = 1043.1
                        assertThat(result.rawAP()).isCloseTo(1043.1, within(0.001));
                        assertThat(result.normalizedAP()).isEqualByComparingTo(0.60);
                }

                @Test
                void zeroAccuracy_givesNearZeroAP() {
                        List<CurvePoint> points = List.of(
                                        point(0.0, 0.0),
                                        point(1.0, 1.0));
                        when(curvePointRepository.findByCurveIdOrderByXAsc(scoreCurve.getId()))
                                        .thenReturn(points);

                        APResult result = apCalculationService.calculateRawAP(
                                        0.0, 10, scoreCurve);

                        assertThat(result.rawAP()).isCloseTo(0.0, within(0.001));
                }

                @Test
                void higherComplexity_givesHigherAP() {
                        List<CurvePoint> points = List.of(
                                        point(0.0, 0.0),
                                        point(1.0, 1.0));
                        when(curvePointRepository.findByCurveIdOrderByXAsc(scoreCurve.getId()))
                                        .thenReturn(points);

                        Double accuracy = 0.90;
                        Double lowComplexity = 5.0;
                        Double highComplexity = 15.0;

                        APResult lowResult = apCalculationService.calculateRawAP(accuracy, lowComplexity, scoreCurve);
                        APResult highResult = apCalculationService.calculateRawAP(accuracy, highComplexity, scoreCurve);

                        assertThat(highResult.rawAP()).isGreaterThan(lowResult.rawAP());
                }
        }

        @Nested
        class WeightedAPCalculation {

                @Test
                void position1_givesNearFullAP() {
                        Double rawAP = 100.0;

                        Double weightedAP = apCalculationService.calculateWeightedAP(rawAP, 1, weightCurve);

                        assertThat(weightedAP).isCloseTo(98.912, within(0.01));
                }

                @Test
                void position2_appliesSigmoidDecay() {
                        Double rawAP = 100.0;

                        Double weightedAP = apCalculationService.calculateWeightedAP(rawAP, 2, weightCurve);

                        assertThat(weightedAP).isCloseTo(97.332, within(0.01));
                }

                @Test
                void higherPosition_givesLowerWeight() {
                        Double rawAP = 100.0;

                        Double pos1 = apCalculationService.calculateWeightedAP(rawAP, 1, weightCurve);
                        Double pos5 = apCalculationService.calculateWeightedAP(rawAP, 5, weightCurve);
                        Double pos10 = apCalculationService.calculateWeightedAP(rawAP, 10, weightCurve);

                        assertThat(pos1).isGreaterThan(pos5);
                        assertThat(pos5).isGreaterThan(pos10);
                }

                @Test
                void position10_matchesExpectedSigmoid() {
                        Double rawAP = 100.0;

                        Double weightedAP = apCalculationService.calculateWeightedAP(rawAP, 10, weightCurve);

                        assertThat(weightedAP).isCloseTo(45.48, within(0.1));
                }

                @Test
                void position15_matchesTargetWeight() {
                        Double rawAP = 100.0;

                        Double weightedAP = apCalculationService.calculateWeightedAP(rawAP, 15, weightCurve);

                        assertThat(weightedAP).isCloseTo(10.0, within(0.01));
                }
        }

        @Nested
        class CacheManagement {

                @Test
                void cachesPointsAfterFirstLoad() {
                        List<CurvePoint> points = List.of(
                                        point(0.0, 0.0),
                                        point(1.0, 1.0));
                        when(curvePointRepository.findByCurveIdOrderByXAsc(scoreCurve.getId()))
                                        .thenReturn(points);

                        apCalculationService.interpolate(scoreCurve, 0.5);
                        apCalculationService.interpolate(scoreCurve, 0.75);

                        verify(curvePointRepository, times(1)).findByCurveIdOrderByXAsc(scoreCurve.getId());
                }

                @Test
                void evictCurveCache_forcesReload() {
                        List<CurvePoint> points = List.of(
                                        point(0.0, 0.0),
                                        point(1.0, 1.0));
                        when(curvePointRepository.findByCurveIdOrderByXAsc(scoreCurve.getId()))
                                        .thenReturn(points);

                        apCalculationService.interpolate(scoreCurve, 0.5);
                        apCalculationService.evictCurveCache(scoreCurve.getId());

                        List<CurvePoint> updatedPoints = List.of(
                                        point(0.0, 0.0),
                                        point(1.0, 0.8));
                        when(curvePointRepository.findByCurveIdOrderByXAsc(scoreCurve.getId()))
                                        .thenReturn(updatedPoints);

                        Double result = apCalculationService.interpolate(scoreCurve, 1.0);

                        assertThat(result).isEqualByComparingTo(0.8);
                }
        }

        @Nested
        class CalculateRawApForOneWeightedGain {

                @Test
                void emptyHistoryReturnsTinyRawAp() {
                        Double result = apCalculationService
                                        .calculateRawApForOneWeightedGain(List.of(), weightCurve);
                        assertThat(result).isLessThan(2.0).isGreaterThan(0.5);
                }

                @Test
                void singleHighPlayMakesOneGainCheap() {
                        // One play of 1100 raw - top is dominated by it. Adding a small play at
                        // position 2 still gains > 1 weighted easily.
                        Double result = apCalculationService.calculateRawApForOneWeightedGain(
                                        List.of(1100.0), weightCurve);
                        assertThat(result).isLessThan(10.0);
                }

                @Test
                void deepConsistentPlayerNeedsHighRawToGainOne() {
                        // 50 plays clustered near 1000 raw - to gain 1 weighted you need
                        // a play comparable to the top.
                        List<Double> plays = new java.util.ArrayList<>();
                        for (int i = 0; i < 50; i++) {
                                plays.add(Double.valueOf(1000 - i));
                        }
                        Double result = apCalculationService
                                        .calculateRawApForOneWeightedGain(plays, weightCurve);
                        assertThat(result).isGreaterThan(700.0);
                }

                @Test
                void monotonicInPlayCount_higherCountMeansHigherRawNeeded() {
                        List<Double> few = List.of(
                                        900.0, 800.0, 700.0);
                        List<Double> many = new java.util.ArrayList<>();
                        for (int i = 0; i < 30; i++) {
                                many.add(Double.valueOf(900 - i * 5));
                        }
                        Double fewResult = apCalculationService
                                        .calculateRawApForOneWeightedGain(few, weightCurve);
                        Double manyResult = apCalculationService
                                        .calculateRawApForOneWeightedGain(many, weightCurve);
                        assertThat(manyResult).isGreaterThan(fewResult);
                }

                @Test
                void resultIsConsistentWithTotalDelta() {
                        // Sanity: applying the returned raw should add ~1 weighted to the total
                        List<Double> plays = List.of(
                                        1000.0, 950.0,
                                        900.0, 850.0);
                        double before = totalWeighted(plays);
                        Double raw = apCalculationService
                                        .calculateRawApForOneWeightedGain(plays, weightCurve);
                        List<Double> after = new java.util.ArrayList<>(plays);
                        insertSorted(after, raw);
                        double afterTotal = totalWeighted(after);
                        assertThat(afterTotal - before).isCloseTo(1.0, within(0.01));
                }

                private double totalWeighted(List<Double> sortedDesc) {
                        double total = 0;
                        for (int i = 0; i < sortedDesc.size(); i++) {
                                total += sortedDesc.get(i)
                                                * apCalculationService
                                                                .calculateWeightedAP(1.0, i, weightCurve);
                        }
                        return total;
                }

                private void insertSorted(List<Double> list, Double value) {
                        int i = 0;
                        while (i < list.size() && list.get(i).compareTo(value) >= 0) {
                                i++;
                        }
                        list.add(i, value);
                }
        }
}
