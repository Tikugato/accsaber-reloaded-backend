package com.accsaber.backend.service.milestone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyInt;
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

import com.accsaber.backend.model.dto.response.milestone.LevelResponse;
import com.accsaber.backend.model.entity.Curve;
import com.accsaber.backend.model.entity.CurveType;
import com.accsaber.backend.model.entity.milestone.LevelThreshold;
import com.accsaber.backend.repository.CurveRepository;
import com.accsaber.backend.repository.milestone.LevelThresholdRepository;

@ExtendWith(MockitoExtension.class)
class LevelServiceTest {

    @Mock
    private LevelThresholdRepository levelThresholdRepository;

    @Mock
    private CurveRepository curveRepository;

    @InjectMocks
    private LevelService service;

    private static final UUID LEVEL_CURVE_ID = UUID.fromString("acc00000-0000-0000-0000-000000000004");

    @BeforeEach
    void setUp() {
        Curve levelCurve = Curve.builder()
                .id(LEVEL_CURVE_ID)
                .name("AccSaber Level Curve")
                .type(CurveType.FORMULA)
                .formula("POWER_FLOOR")
                .xParameterName("base")
                .xParameterValue((double) (52))
                .yParameterName("exponent")
                .yParameterValue((double) (1.2))
                .build();
        when(curveRepository.findById(LEVEL_CURVE_ID)).thenReturn(Optional.of(levelCurve));
        service.evictLevelCurveCache();
    }

    @Nested
    class XpForLevel {

        @Test
        void level1_returnsExpectedValue() {
            // floor(52 * 1^1.2) = 52
            assertThat(service.xpForLevel(1)).isEqualByComparingTo((double) (52));
        }

        @Test
        void level2_returnsExpectedValue() {
            // floor(52 * 2^1.2) = floor(52 * 2.2974) = floor(119.46) = 119
            assertThat((int) (service.xpForLevel(2))).isEqualTo(119);
        }

        @Test
        void level10_returnsExpectedValue() {
            // floor(52 * 10^1.2) = floor(52 * 15.8489) = floor(824.14) = 824
            assertThat((int) (service.xpForLevel(10))).isEqualTo(824);
        }

        @Test
        void level100_returnsCapValue() {
            Double level100Cost = service.xpForLevel(100);
            // floor(52 * 100^1.2) = floor(52 * 251.189) = 13061
            assertThat(level100Cost.intValue()).isEqualTo(13061);
        }

        @Test
        void level101_sameAsLevel100_flatCap() {
            assertThat(service.xpForLevel(101)).isEqualByComparingTo(service.xpForLevel(100));
        }

        @Test
        void level200_sameAsLevel100_flatCap() {
            assertThat(service.xpForLevel(200)).isEqualByComparingTo(service.xpForLevel(100));
        }

        @Test
        void level999_sameAsLevel100_flatCap() {
            assertThat(service.xpForLevel(999)).isEqualByComparingTo(service.xpForLevel(100));
        }
    }

    @Nested
    class CalculateLevel {

        @Test
        void zeroXp_returnsLevel0() {
            LevelResponse response = service.calculateLevel(0.0);

            assertThat(response.getLevel()).isEqualTo(0);
            assertThat(response.getTotalXp()).isEqualByComparingTo(0.0);
            assertThat(response.getProgressPercent()).isEqualByComparingTo(0.0);
            assertThat(response.getTitle()).isNull();
        }

        @Test
        void nullXp_returnsLevel0() {
            LevelResponse response = service.calculateLevel(null);

            assertThat(response.getLevel()).isEqualTo(0);
        }

        @Test
        void exactlyEnoughForLevel1_returnsLevel1() {
            Double totalXp = (double) (52);
            when(levelThresholdRepository.findHighestTitleAtOrBelow(1))
                    .thenReturn(Optional.of(LevelThreshold.builder().level(0).title("Newcomer").build()));

            LevelResponse response = service.calculateLevel(totalXp);

            assertThat(response.getLevel()).isEqualTo(1);
            assertThat(response.getTitle()).isEqualTo("Newcomer");
            assertThat(response.getXpForCurrentLevel()).isEqualByComparingTo(0.0);
        }

        @Test
        void justUnderLevel1_returnsLevel0() {
            Double totalXp = (double) (51);
            when(levelThresholdRepository.findHighestTitleAtOrBelow(0))
                    .thenReturn(Optional.of(LevelThreshold.builder().level(0).title("Newcomer").build()));

            LevelResponse response = service.calculateLevel(totalXp);

            assertThat(response.getLevel()).isEqualTo(0);
        }

        @Test
        void progressPercent_isCalculatedCorrectly() {
            // level1 = 52, level2 = 119 → at 52 + 59 = 111 → 59/119 ≈ 49.58%
            Double totalXp = (double) (111);
            when(levelThresholdRepository.findHighestTitleAtOrBelow(anyInt()))
                    .thenReturn(Optional.empty());

            LevelResponse response = service.calculateLevel(totalXp);

            assertThat(response.getLevel()).isEqualTo(1);
            assertThat(response.getProgressPercent()).isCloseTo(49.58, within(1.0));
        }

        @Test
        void xpForNextLevel_reportedCorrectly() {
            Double totalXp = (double) (52);
            when(levelThresholdRepository.findHighestTitleAtOrBelow(anyInt()))
                    .thenReturn(Optional.empty());

            LevelResponse response = service.calculateLevel(totalXp);
            assertThat(response.getXpForNextLevel()).isEqualByComparingTo(service.xpForLevel(2));
        }

        @Test
        void highLevel_titleFromHighestMatchingThreshold() {
            Double bigXp = (double) (5_000_000);
            LevelThreshold grandmaster = LevelThreshold.builder()
                    .level(60).title("Grandmaster").build();
            when(levelThresholdRepository.findHighestTitleAtOrBelow(anyInt()))
                    .thenReturn(Optional.of(grandmaster));

            LevelResponse response = service.calculateLevel(bigXp);

            assertThat(response.getLevel()).isGreaterThan(60);
            assertThat(response.getTitle()).isEqualTo("Grandmaster");
        }

        @Test
        void infiniteLevels_noCapOnLevel() {
            Double massiveXp = (double) (1_000_000_000);
            when(levelThresholdRepository.findHighestTitleAtOrBelow(anyInt()))
                    .thenReturn(Optional.empty());

            LevelResponse response = service.calculateLevel(massiveXp);

            assertThat(response.getLevel()).isGreaterThan(100);
        }

        @Test
        void aboveLevel100_xpForNextUsesLevel100Cost() {
            Double level100Cost = service.xpForLevel(100);

            Double cumulativeFor101 = 0.0;
            for (int i = 1; i <= 101; i++) {
                cumulativeFor101 = (cumulativeFor101 + service.xpForLevel(i));
            }

            when(levelThresholdRepository.findHighestTitleAtOrBelow(anyInt()))
                    .thenReturn(Optional.empty());

            LevelResponse response = service.calculateLevel(cumulativeFor101);

            assertThat(response.getLevel()).isEqualTo(101);
            assertThat(response.getXpForNextLevel()).isEqualByComparingTo(level100Cost);
        }

        @Test
        void totalXp_setInResponse() {
            Double totalXp = (double) (500);
            when(levelThresholdRepository.findHighestTitleAtOrBelow(anyInt()))
                    .thenReturn(Optional.empty());

            LevelResponse response = service.calculateLevel(totalXp);

            assertThat(response.getTotalXp()).isEqualByComparingTo(totalXp);
        }
    }
}
