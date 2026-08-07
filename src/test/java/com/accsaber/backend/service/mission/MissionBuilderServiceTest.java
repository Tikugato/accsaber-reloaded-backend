package com.accsaber.backend.service.mission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.accsaber.backend.model.entity.map.MapDifficulty;
import com.accsaber.backend.model.entity.mission.MissionBand;
import com.accsaber.backend.model.entity.score.Score;

class MissionBuilderServiceTest {

    private MissionSkillService skillService;
    private MissionBuilderService service;

    @BeforeEach
    void setUp() {
        skillService = mock(MissionSkillService.class);
        service = new MissionBuilderService(null, null, null, null, skillService);
    }

    @Test
    void pbAboveThresholdAnchorIndexLocksToHundredScoreCutoffOncePastHundred() throws Exception {
        assertThat(invokeAnchorIndex(150, MissionBand.easy, 0.70)).isEqualTo(70);
        assertThat(invokeAnchorIndex(150, MissionBand.medium, 0.45)).isEqualTo(45);
        assertThat(invokeAnchorIndex(150, MissionBand.hard, 0.22)).isEqualTo(22);
        assertThat(invokeAnchorIndex(150, MissionBand.extreme, 0.10)).isEqualTo(9);
    }

    @Test
    void pbAboveThresholdAnchorIndexKeepsPercentileLogicAtOrBelowHundred() throws Exception {
        assertThat(invokeAnchorIndex(100, MissionBand.easy, 0.70)).isEqualTo(70);
        assertThat(invokeAnchorIndex(80, MissionBand.medium, 0.45)).isEqualTo(36);
        assertThat(invokeAnchorIndex(60, MissionBand.hard, 0.22)).isEqualTo(13);
        assertThat(invokeAnchorIndex(50, MissionBand.extreme, 0.10)).isEqualTo(5);
    }

    @Test
    void pbAboveThresholdAvailabilityCapLocksToHundredScoreCutoffOncePastHundred() throws Exception {
        BigDecimal threshold = new BigDecimal("1000");
        List<Score> scores = descendingScores(150, 1200);

        BigDecimal hardCap = invokeAvailabilityCap(scores, MissionBand.hard, threshold);
        BigDecimal extremeCap = invokeAvailabilityCap(scores, MissionBand.extreme, threshold);

        assertThat(hardCap).isEqualByComparingTo(new BigDecimal("1000"));
        assertThat(extremeCap).isEqualByComparingTo(new BigDecimal("1000"));
    }

    @Test
    void complexityAwareScoreTargetCapUsesRepresentativeApFromSameComplexityBand() throws Exception {
        Long userId = 42L;
        UUID categoryId = UUID.randomUUID();
        MapPick pick = new MapPick(MapDifficulty.builder().id(UUID.randomUUID()).build(), new BigDecimal("11.2"), 1000);
        when(skillService.representativeUserApForComplexityBand(
                eq(userId), eq(categoryId), eq(new BigDecimal("10.0")), eq(new BigDecimal("13.0"))))
                .thenReturn(new BigDecimal("925"));

        BigDecimal capped = invokeComplexityCap(userId, categoryId, pick, MissionBand.medium, new BigDecimal("1010"));

        assertThat(capped).isEqualByComparingTo(new BigDecimal("980.50"));
    }

    @Test
    void complexityAwareScoreTargetCapLeavesTargetAloneWhenNoComplexityBandHistoryExists() throws Exception {
        Long userId = 42L;
        UUID categoryId = UUID.randomUUID();
        MapPick pick = new MapPick(MapDifficulty.builder().id(UUID.randomUUID()).build(), new BigDecimal("11.2"), 1000);
        when(skillService.representativeUserApForComplexityBand(
                eq(userId), eq(categoryId), eq(new BigDecimal("10.0")), eq(new BigDecimal("13.0"))))
                .thenReturn(null);

        BigDecimal capped = invokeComplexityCap(userId, categoryId, pick, MissionBand.medium, new BigDecimal("1010"));

        assertThat(capped).isEqualByComparingTo(new BigDecimal("1010"));
    }

    private int invokeAnchorIndex(int scoreCount, MissionBand band, double percentile) throws Exception {
        Method method = MissionBuilderService.class.getDeclaredMethod(
                "pbAboveThresholdAnchorIndex", int.class, MissionBand.class, double.class);
        method.setAccessible(true);
        return (int) method.invoke(service, scoreCount, band, percentile);
    }

    private BigDecimal invokeAvailabilityCap(List<Score> scores, MissionBand band, BigDecimal threshold) throws Exception {
        Method method = MissionBuilderService.class.getDeclaredMethod(
                "pbAboveThresholdAvailabilityCap", List.class, MissionBand.class, BigDecimal.class);
        method.setAccessible(true);
        return (BigDecimal) method.invoke(service, scores, band, threshold);
    }

    private BigDecimal invokeComplexityCap(Long userId, UUID categoryId, MapPick pick,
            MissionBand band, BigDecimal targetRawAp) throws Exception {
        Method method = MissionBuilderService.class.getDeclaredMethod(
                "applyComplexityAwareScoreTargetCap",
                Long.class, UUID.class, MapPick.class, MissionBand.class, BigDecimal.class);
        method.setAccessible(true);
        return (BigDecimal) method.invoke(service, userId, categoryId, pick, band, targetRawAp);
    }

    private List<Score> descendingScores(int count, int topAp) {
        List<Score> scores = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            scores.add(Score.builder().ap(BigDecimal.valueOf(topAp - i)).build());
        }
        return scores;
    }
}
