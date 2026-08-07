package com.accsaber.backend.service.score;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
class ApPipelineGoldenTest {

        private static final MathContext MC = new MathContext(16, RoundingMode.HALF_UP);
        private static final int AP_SCALE = 6;
        private static final int CURVE_POINTS = 1000;

        @Mock
        private CurvePointRepository curvePointRepository;

        @InjectMocks
        private APCalculationService apCalculationService;

        private Curve scoreCurve;
        private Curve weightCurve;
        private TreeMap<BigDecimal, BigDecimal> referenceCurve;

        @BeforeEach
        void setUp() {
                apCalculationService.evictAllCurveCaches();

                scoreCurve = Curve.builder()
                                .id(UUID.randomUUID())
                                .name("Golden Score Curve")
                                .type(CurveType.POINT_LOOKUP)
                                .scale(61.0)
                                .shift(-18.0)
                                .build();

                weightCurve = Curve.builder()
                                .id(UUID.randomUUID())
                                .name("Golden Weight Curve")
                                .type(CurveType.FORMULA)
                                .formula("LOGISTIC_SIGMOID")
                                .xParameterName("k")
                                .xParameterValue(0.08)
                                .yParameterName("y1")
                                .yParameterValue(0.85)
                                .zParameterName("x1")
                                .zParameterValue(20.0)
                                .build();

                List<CurvePoint> points = new ArrayList<>(CURVE_POINTS);
                referenceCurve = new TreeMap<>();
                for (int i = 0; i < CURVE_POINTS; i++) {
                        double x = 0.60 + (0.40 * i) / (CURVE_POINTS - 1);
                        double y = Math.pow(x, 30);
                        BigDecimal bx = new BigDecimal(String.format(java.util.Locale.ROOT, "%.16f", x));
                        BigDecimal by = new BigDecimal(String.format(java.util.Locale.ROOT, "%.16f", y));
                        referenceCurve.put(bx, by);
                        points.add(CurvePoint.builder().x(bx.doubleValue()).y(by.doubleValue()).build());
                }
                org.mockito.Mockito.lenient()
                                .when(curvePointRepository.findByCurveIdOrderByXAsc(scoreCurve.getId()))
                                .thenReturn(points);
        }

        private BigDecimal referenceInterpolate(BigDecimal accuracy) {
                Map.Entry<BigDecimal, BigDecimal> floor = referenceCurve.floorEntry(accuracy);
                Map.Entry<BigDecimal, BigDecimal> ceiling = referenceCurve.ceilingEntry(accuracy);
                if (floor == null)
                        return ceiling.getValue();
                if (ceiling == null)
                        return floor.getValue();
                if (floor.getKey().compareTo(ceiling.getKey()) == 0)
                        return floor.getValue();
                BigDecimal x0 = floor.getKey();
                BigDecimal y0 = floor.getValue();
                BigDecimal dx = accuracy.subtract(x0);
                BigDecimal range = ceiling.getKey().subtract(x0);
                BigDecimal dy = ceiling.getValue().subtract(y0);
                return y0.add(dx.multiply(dy, MC).divide(range, MC));
        }

        private BigDecimal referenceRawAp(BigDecimal accuracy, BigDecimal complexity) {
                return referenceInterpolate(accuracy)
                                .multiply(complexity.subtract(BigDecimal.valueOf(scoreCurve.getShift())), MC)
                                .multiply(BigDecimal.valueOf(scoreCurve.getScale()), MC)
                                .setScale(AP_SCALE, RoundingMode.HALF_UP);
        }

        private BigDecimal referenceWeightedAp(BigDecimal rawAp, int position) {
                double k = weightCurve.getXParameterValue();
                double y1 = weightCurve.getYParameterValue();
                double x1 = weightCurve.getZParameterValue();
                double x0 = -Math.log((1 - y1) / (y1 * Math.exp(k * x1) - 1)) / k;
                double weight = (1 + Math.exp(-k * x0)) / (1 + Math.exp(k * (position - x0)));
                return rawAp.multiply(new BigDecimal(weight, MC), MC)
                                .setScale(AP_SCALE, RoundingMode.HALF_UP);
        }

        @Nested
        class MatchesLegacyBigDecimalPipeline {

                @Test
                void rawAp_matchesAcrossAccuracyAndComplexityGrid() {
                        int compared = 0;
                        for (int a = 0; a <= 400; a++) {
                                double accuracy = 0.6005 + a * 0.000997;
                                for (int c = 1; c <= 20; c++) {
                                        double complexity = c * 1.37;
                                        APResult actual = apCalculationService.calculateRawAP(
                                                        accuracy, complexity, scoreCurve);
                                        BigDecimal expected = referenceRawAp(
                                                        BigDecimal.valueOf(accuracy), BigDecimal.valueOf(complexity));
                                        assertThat(actual.rawAP())
                                                        .as("accuracy=%s complexity=%s", accuracy, complexity)
                                                        .isEqualTo(expected.doubleValue());
                                        compared++;
                                }
                        }
                        assertThat(compared).isEqualTo(401 * 20);
                }

                @Test
                void weightedAp_matchesAcrossPositions() {
                        for (int position = 0; position < 3000; position++) {
                                double rawAp = 50.0 + (position % 950);
                                double actual = apCalculationService.calculateWeightedAP(
                                                rawAp, position, weightCurve);
                                BigDecimal expected = referenceWeightedAp(
                                                BigDecimal.valueOf(rawAp).setScale(AP_SCALE, RoundingMode.HALF_UP),
                                                position);
                                assertThat(actual)
                                                .as("position=%s rawAp=%s", position, rawAp)
                                                .isEqualTo(expected.doubleValue());
                        }
                }

                @Test
                void summedWeightedAp_matchesLegacyTotalAtStoredPrecision() {
                        for (int n : new int[] { 1, 25, 100, 500, 2000 }) {
                                double actualTotal = 0.0;
                                BigDecimal expectedTotal = BigDecimal.ZERO;
                                for (int i = 0; i < n; i++) {
                                        double rawAp = 1000.0 - i * 0.37;
                                        actualTotal += apCalculationService.calculateWeightedAP(
                                                        rawAp, i, weightCurve);
                                        expectedTotal = expectedTotal.add(referenceWeightedAp(
                                                        BigDecimal.valueOf(rawAp).setScale(AP_SCALE,
                                                                        RoundingMode.HALF_UP),
                                                        i), MC);
                                }
                                assertThat(com.accsaber.backend.util.Rounding.round(actualTotal, AP_SCALE))
                                                .as("n=%s", n)
                                                .isEqualTo(expectedTotal.setScale(AP_SCALE, RoundingMode.HALF_UP)
                                                                .doubleValue());
                        }
                }
        }
}
