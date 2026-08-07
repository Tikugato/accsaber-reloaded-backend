package com.accsaber.backend.service.map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.accsaber.backend.model.dto.response.map.MapDifficultyStatisticsResponse;
import com.accsaber.backend.model.entity.map.Difficulty;
import com.accsaber.backend.model.entity.map.MapDifficulty;
import com.accsaber.backend.model.entity.map.MapDifficultyStatistics;
import com.accsaber.backend.model.entity.map.MapDifficultyStatus;
import com.accsaber.backend.repository.map.MapDifficultyStatisticsRepository;
import com.accsaber.backend.repository.score.ScoreRepository;

@ExtendWith(MockitoExtension.class)
class MapDifficultyStatisticsServiceTest {

        @Mock
        private MapDifficultyStatisticsRepository statisticsRepository;

        @Mock
        private ScoreRepository scoreRepository;

        @InjectMocks
        private MapDifficultyStatisticsService statisticsService;

        @Nested
        class FindActive {

                @Test
                void returnsResponse_whenActiveStatsExist() {
                        UUID diffId = UUID.randomUUID();
                        MapDifficulty diff = buildDifficulty(diffId);
                        MapDifficultyStatistics stats = buildStats(diff,
                                        900.0, 100.0, 500.0, 10, true);
                        when(statisticsRepository.findByMapDifficultyIdAndActiveTrue(diffId))
                                        .thenReturn(Optional.of(stats));

                        Optional<MapDifficultyStatisticsResponse> result = statisticsService.findActive(diffId);

                        assertThat(result).isPresent();
                        assertThat(result.get().getMaxAp()).isEqualByComparingTo(900.0);
                        assertThat(result.get().getMinAp()).isEqualByComparingTo(100.0);
                        assertThat(result.get().getAverageAp()).isEqualByComparingTo(500.0);
                        assertThat(result.get().getTotalScores()).isEqualTo(10);
                }

                @Test
                void returnsEmpty_whenNoActiveStats() {
                        UUID diffId = UUID.randomUUID();
                        when(statisticsRepository.findByMapDifficultyIdAndActiveTrue(diffId))
                                        .thenReturn(Optional.empty());

                        Optional<MapDifficultyStatisticsResponse> result = statisticsService.findActive(diffId);

                        assertThat(result).isEmpty();
                }
        }

        @Nested
        class FindActiveForDifficulties {

                @Test
                void returnsMapOfResponsesKeyedByDifficultyId() {
                        UUID id1 = UUID.randomUUID();
                        UUID id2 = UUID.randomUUID();
                        MapDifficulty d1 = buildDifficulty(id1);
                        MapDifficulty d2 = buildDifficulty(id2);
                        List<MapDifficultyStatistics> statsList = List.of(
                                        buildStats(d1, 800.0, 200.0,
                                                        500.0, 5, true),
                                        buildStats(d2, 600.0, 150.0,
                                                        400.0, 3, true));
                        when(statisticsRepository.findActiveByMapDifficultyIdIn(List.of(id1, id2)))
                                        .thenReturn(statsList);
                        when(scoreRepository.findCurrentTopOnes(List.of(id1, id2)))
                                        .thenReturn(Collections.emptyList());

                        Map<UUID, MapDifficultyStatisticsResponse> result = statisticsService
                                        .findActiveForDifficulties(List.of(id1, id2));

                        assertThat(result).containsKeys(id1, id2);
                        assertThat(result.get(id1).getMaxAp()).isEqualByComparingTo(800.0);
                        assertThat(result.get(id2).getTotalScores()).isEqualTo(3);
                }

        }

        @Nested
        class UpdateStatistics {

                @Test
                void noExistingStats_createsActiveRecord_withNoSupersedes() {
                        MapDifficulty diff = buildDifficulty(UUID.randomUUID());
                        when(statisticsRepository.findByMapDifficultyIdAndActiveTrue(diff.getId()))
                                        .thenReturn(Optional.empty());
                        when(statisticsRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

                        statisticsService.updateStatistics(diff,
                                        900.0, 100.0, 500.0, 10, 1L);

                        ArgumentCaptor<MapDifficultyStatistics> captor = ArgumentCaptor
                                        .forClass(MapDifficultyStatistics.class);
                        verify(statisticsRepository).saveAndFlush(captor.capture());
                        MapDifficultyStatistics saved = captor.getValue();
                        assertThat(saved.isActive()).isTrue();
                        assertThat(saved.getSupersedes()).isNull();
                        assertThat(saved.getMaxAp()).isEqualByComparingTo(900.0);
                        assertThat(saved.getTotalScores()).isEqualTo(10);
                }

                @Test
                void existingStats_deactivatesOldRecord() {
                        MapDifficulty diff = buildDifficulty(UUID.randomUUID());
                        MapDifficultyStatistics existing = buildStats(diff,
                                        500.0, 100.0, 300.0, 5, true);
                        when(statisticsRepository.findByMapDifficultyIdAndActiveTrue(diff.getId()))
                                        .thenReturn(Optional.of(existing));
                        when(statisticsRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

                        statisticsService.updateStatistics(diff,
                                        900.0, 200.0, 600.0, 15, 7L);

                        assertThat(existing.isActive()).isFalse();
                        verify(statisticsRepository, times(2)).saveAndFlush(any());
                }

                @Test
                void newVersion_linksToOldViaSupersedes_andCarriesCorrectValues() {
                        MapDifficulty diff = buildDifficulty(UUID.randomUUID());
                        MapDifficultyStatistics existing = buildStats(diff,
                                        500.0, 100.0, 300.0, 5, true);
                        when(statisticsRepository.findByMapDifficultyIdAndActiveTrue(diff.getId()))
                                        .thenReturn(Optional.of(existing));
                        when(statisticsRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

                        statisticsService.updateStatistics(diff,
                                        900.0, 200.0, 600.0, 15, 7L);

                        ArgumentCaptor<MapDifficultyStatistics> captor = ArgumentCaptor
                                        .forClass(MapDifficultyStatistics.class);
                        verify(statisticsRepository, times(2)).saveAndFlush(captor.capture());
                        MapDifficultyStatistics newVersion = captor.getAllValues().get(1);
                        assertThat(newVersion.getSupersedes()).isEqualTo(existing);
                        assertThat(newVersion.isActive()).isTrue();
                        assertThat(newVersion.getMaxAp()).isEqualByComparingTo(900.0);
                        assertThat(newVersion.getMinAp()).isEqualByComparingTo(200.0);
                        assertThat(newVersion.getTotalScores()).isEqualTo(15);
                        assertThat(newVersion.getSupersedesAuthor()).isEqualTo(7L);
                }
        }

        private MapDifficulty buildDifficulty(UUID id) {
                return MapDifficulty.builder()
                                .id(id)
                                .difficulty(Difficulty.EXPERT_PLUS)
                                .characteristic("Standard")
                                .status(MapDifficultyStatus.RANKED)
                                .active(true)
                                .build();
        }

        private MapDifficultyStatistics buildStats(MapDifficulty diff, Double maxAp, Double minAp,
                        Double averageAp, int totalScores, boolean active) {
                return MapDifficultyStatistics.builder()
                                .id(UUID.randomUUID())
                                .mapDifficulty(diff)
                                .maxAp(maxAp)
                                .minAp(minAp)
                                .averageAp(averageAp)
                                .totalScores(totalScores)
                                .active(active)
                                .build();
        }
}
