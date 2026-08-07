package com.accsaber.backend.service.score;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.accsaber.backend.model.entity.Curve;
import com.accsaber.backend.model.entity.CurveType;
import com.accsaber.backend.repository.CurveRepository;

@ExtendWith(MockitoExtension.class)
class XPCalculationServiceTest {

    @Mock
    private APCalculationService apCalculationService;

    @Mock
    private CurveRepository curveRepository;

    @InjectMocks
    private XPCalculationService service;

    private static final UUID XP_CURVE_ID = UUID.fromString("acc00000-0000-0000-0000-000000000003");
    private static final Double COMPLEXITY_10 = (double) (10);
    private static final Double COMPLEXITY_12 = (double) (12);
    private Curve xpCurve;

    @BeforeEach
    void setUp() {
        service.evictXpCurveCache();

        xpCurve = Curve.builder()
                .id(XP_CURVE_ID)
                .name("XP Curve")
                .type(CurveType.POINT_LOOKUP)
                .build();

        ReflectionTestUtils.setField(service, "baseXpPerScore", 25);
        ReflectionTestUtils.setField(service, "maxBonusXpPerScore", 1000);
        ReflectionTestUtils.setField(service, "improvementMultiplier", 1.5);

        org.mockito.Mockito.clearInvocations(apCalculationService);
    }

    @Nested
    class CalculateXpForNewMap {

        @Test
        void baseXpAlwaysIncluded() {
            when(curveRepository.findById(XP_CURVE_ID)).thenReturn(Optional.of(xpCurve));
            when(apCalculationService.interpolate(xpCurve, 0.0))
                    .thenReturn(0.0);

            Double result = service.calculateXpForNewMap(0.0, COMPLEXITY_10);

            assertThat(result).isEqualByComparingTo((double) (25));
        }

        @Test
        void topAccuracy_givesBaseXpPlusMaxBonus() {
            when(curveRepository.findById(XP_CURVE_ID)).thenReturn(Optional.of(xpCurve));
            when(apCalculationService.interpolate(xpCurve, 1.0))
                    .thenReturn(1.0);

            Double result = service.calculateXpForNewMap(1.0, COMPLEXITY_10);

            assertThat(result).isEqualByComparingTo((double) (1025));
        }

        @Test
        void complexityMultiplier_scalesBonus() {
            when(curveRepository.findById(XP_CURVE_ID)).thenReturn(Optional.of(xpCurve));
            when(apCalculationService.interpolate(xpCurve, 0.95))
                    .thenReturn(0.18);

            Double result = service.calculateXpForNewMap(0.95, COMPLEXITY_12);

            // 25 + 0.18 * 1000 * cbrt(12/10) = 25 + 191.3 = 216.3
            assertThat(result).isCloseTo(216.3, within(1.0));
        }

        @Test
        void midAccuracy_returnsCorrectXp() {
            when(curveRepository.findById(XP_CURVE_ID)).thenReturn(Optional.of(xpCurve));
            when(apCalculationService.interpolate(xpCurve, 0.97))
                    .thenReturn(0.36);

            Double result = service.calculateXpForNewMap(0.97, COMPLEXITY_10);

            assertThat(result).isCloseTo(385.0, within(1.0));
        }
    }

    @Nested
    class CalculateXpForImprovement {

        @Test
        void improvement_getsBaseXpPlusBoostedDelta() {
            when(curveRepository.findById(XP_CURVE_ID)).thenReturn(Optional.of(xpCurve));
            when(apCalculationService.interpolate(xpCurve, 0.99))
                    .thenReturn(0.78);
            when(apCalculationService.interpolate(xpCurve, 0.95))
                    .thenReturn(0.18);

            Double result = service.calculateXpForImprovement(
                    0.99, 0.95, COMPLEXITY_10);

            assertThat(result).isCloseTo(925.0, within(1.0));
        }

        @Test
        void improvement_whenNewCurveBelowOld_deltaIsZero_getsOnlyBaseXp() {
            when(curveRepository.findById(XP_CURVE_ID)).thenReturn(Optional.of(xpCurve));
            when(apCalculationService.interpolate(xpCurve, 0.90))
                    .thenReturn(0.01);
            when(apCalculationService.interpolate(xpCurve, 0.95))
                    .thenReturn(0.18);

            Double result = service.calculateXpForImprovement(
                    0.90, 0.95, COMPLEXITY_10);

            assertThat(result).isEqualByComparingTo((double) (25));
        }

        @Test
        void improvement_withNullOldAccuracy_treatsOldCurveBonusAsZero() {
            when(curveRepository.findById(XP_CURVE_ID)).thenReturn(Optional.of(xpCurve));
            when(apCalculationService.interpolate(xpCurve, 0.95))
                    .thenReturn(0.18);

            Double result = service.calculateXpForImprovement(
                    0.95, null, COMPLEXITY_10);

            assertThat(result).isCloseTo(295.0, within(1.0));
        }

        @Test
        void chainedImprovements_xpDoesNotInflate() {
            when(curveRepository.findById(XP_CURVE_ID)).thenReturn(Optional.of(xpCurve));
            when(apCalculationService.interpolate(xpCurve, 0.95))
                    .thenReturn(0.18);
            when(apCalculationService.interpolate(xpCurve, 0.96))
                    .thenReturn(0.25);
            when(apCalculationService.interpolate(xpCurve, 0.97))
                    .thenReturn(0.36);

            Double xp1 = service.calculateXpForNewMap(0.95, COMPLEXITY_10);
            assertThat(xp1).isCloseTo(205.0, within(1.0));

            Double xp2 = service.calculateXpForImprovement(
                    0.96, 0.95, COMPLEXITY_10);
            assertThat(xp2).isCloseTo(130.0, within(1.0));

            Double xp3 = service.calculateXpForImprovement(
                    0.97, 0.96, COMPLEXITY_10);
            assertThat(xp3).isCloseTo(190.0, within(1.0));

            Double xpNewMapAt97 = service.calculateXpForNewMap(0.97, COMPLEXITY_10);
            assertThat(xp3).isLessThan(xpNewMapAt97);
        }
    }

    @Nested
    class CalculateXpForWorseScore {

        @Test
        void returnsOnlyBaseXp() {
            Double result = service.calculateXpForWorseScore();

            assertThat(result).isEqualByComparingTo((double) (25));
        }

        @Test
        void doesNotCallCurveOrApService() {
            service.calculateXpForWorseScore();

            verify(curveRepository, times(0)).findById(any());
            verify(apCalculationService, times(0)).interpolate(any(), anyDouble());
        }
    }

    @Nested
    class CurveCaching {

        @Test
        void xpCurveLoadedOnce_cachedOnSubsequentCalls() {
            when(curveRepository.findById(XP_CURVE_ID)).thenReturn(Optional.of(xpCurve));
            when(apCalculationService.interpolate(any(), anyDouble())).thenReturn(0.0);

            service.calculateXpForNewMap(0.0, COMPLEXITY_10);
            service.calculateXpForNewMap(0.0, COMPLEXITY_10);
            service.calculateXpForNewMap(0.0, COMPLEXITY_10);

            verify(curveRepository, times(1)).findById(XP_CURVE_ID);
        }

        @Test
        void evictCache_forcesReloadOnNextCall() {
            when(curveRepository.findById(XP_CURVE_ID)).thenReturn(Optional.of(xpCurve));
            when(apCalculationService.interpolate(any(), anyDouble())).thenReturn(0.0);

            service.calculateXpForNewMap(0.0, COMPLEXITY_10);
            service.evictXpCurveCache();
            service.calculateXpForNewMap(0.0, COMPLEXITY_10);

            verify(curveRepository, times(2)).findById(XP_CURVE_ID);
        }

        @Test
        void evictCache_alsoClearsApServiceCache() {
            service.evictXpCurveCache();

            verify(apCalculationService).evictCurveCache(XP_CURVE_ID);
        }
    }

    @Nested
    class ComputeCurveBonus {

        @Test
        void halfNormalizedXp_withReferenceComplexity_returnsHalfOfMax() {
            when(curveRepository.findById(XP_CURVE_ID)).thenReturn(Optional.of(xpCurve));
            when(apCalculationService.interpolate(xpCurve, 0.95))
                    .thenReturn(0.5);

            Double bonus = service.computeCurveBonus(0.95, COMPLEXITY_10);

            // 0.5 * 1000 * (10/10) = 500
            assertThat(bonus).isCloseTo(500.0, within(0.001));
        }

        @Test
        void complexityBelowFloor_clampedToMinimum() {
            when(curveRepository.findById(XP_CURVE_ID)).thenReturn(Optional.of(xpCurve));
            when(apCalculationService.interpolate(xpCurve, 0.95))
                    .thenReturn(0.5);

            Double bonus = service.computeCurveBonus(0.95, 0.2);

            // Clamped to 4.5: 0.5 * 1000 * cbrt(4.5/10) = 0.5 * 1000 * 0.7663 = 383.2
            assertThat(bonus).isCloseTo(383.2, within(1.0));
        }

        @Test
        void complexityAboveReference_scalesUp() {
            when(curveRepository.findById(XP_CURVE_ID)).thenReturn(Optional.of(xpCurve));
            when(apCalculationService.interpolate(xpCurve, 0.95))
                    .thenReturn(0.5);

            Double bonus = service.computeCurveBonus(0.95, COMPLEXITY_12);

            // 0.5 * 1000 * cbrt(12/10) = 0.5 * 1000 * 1.0627 = 531.3
            assertThat(bonus).isCloseTo(531.3, within(1.0));
        }
    }

    @Nested
    class Config {

        @Test
        void getBaseXpPerScore_returnsConfiguredValue() {
            assertThat(service.getBaseXpPerScore()).isEqualTo(25);
        }

        @Test
        void getMaxBonusXpPerScore_returnsConfiguredValue() {
            assertThat(service.getMaxBonusXpPerScore()).isEqualTo(1000);
        }

        @Test
        void getXpCurveId_returnsKnownId() {
            assertThat(service.getXpCurveId())
                    .isEqualTo(UUID.fromString("acc00000-0000-0000-0000-000000000003"));
        }
    }
}
