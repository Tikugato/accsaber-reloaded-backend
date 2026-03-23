package com.accsaber.backend.service.score;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.accsaber.backend.client.BeatLeaderClient;
import com.accsaber.backend.model.dto.APResult;
import com.accsaber.backend.model.dto.platform.beatleader.BeatLeaderScoreResponse;
import com.accsaber.backend.model.entity.Modifier;
import com.accsaber.backend.model.entity.score.Score;
import com.accsaber.backend.model.entity.score.ScoreModifierLink;
import com.accsaber.backend.repository.score.ScoreModifierLinkRepository;
import com.accsaber.backend.repository.score.ScoreRepository;
import com.accsaber.backend.service.map.MapDifficultyComplexityService;
import com.accsaber.backend.service.stats.OverallStatisticsService;
import com.accsaber.backend.service.stats.RankingService;
import com.accsaber.backend.service.stats.StatisticsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreCorrectionService {

    private static final int ACCURACY_SCALE = 10;

    private final ScoreRepository scoreRepository;
    private final ScoreModifierLinkRepository modifierLinkRepository;
    private final BeatLeaderClient beatLeaderClient;
    private final APCalculationService apCalculationService;
    private final MapDifficultyComplexityService mapComplexityService;
    private final StatisticsService statisticsService;
    private final OverallStatisticsService overallStatisticsService;
    private final RankingService rankingService;
    private final ScoreRankingService scoreRankingService;

    @Autowired
    @Qualifier("backfillExecutor")
    private Executor backfillExecutor;

    @Async("taskExecutor")
    public void correctModifierScoresAsync() {
        log.info("Starting modifier score correction");

        List<Score> scores = scoreRepository.findActiveScoresWithModifiersAndBlScoreId();
        log.info("Found {} active scores with modifiers and BL score IDs to check", scores.size());

        int corrected = 0;
        int skipped = 0;
        int failed = 0;
        ConcurrentHashMap<UUID, Set<Long>> affectedByCategory = new ConcurrentHashMap<>();
        Set<UUID> affectedDifficulties = new HashSet<>();

        int batchSize = 8;
        long delayMs = 1000;

        for (int i = 0; i < scores.size(); i += batchSize) {
            List<Score> batch = scores.subList(i, Math.min(i + batchSize, scores.size()));

            List<CompletableFuture<Boolean>> futures = batch.stream()
                    .map(score -> CompletableFuture.supplyAsync(() -> {
                        try {
                            return correctSingleScore(score);
                        } catch (Exception e) {
                            log.error("Failed to correct score {} (BL {}): {}",
                                    score.getId(), score.getBlScoreId(), e.getMessage());
                            return null;
                        }
                    }, backfillExecutor))
                    .toList();

            for (int j = 0; j < futures.size(); j++) {
                Boolean result = futures.get(j).join();
                Score score = batch.get(j);
                if (result == null) {
                    failed++;
                } else if (result) {
                    corrected++;
                    affectedByCategory.computeIfAbsent(
                            score.getMapDifficulty().getCategory().getId(),
                            k -> ConcurrentHashMap.newKeySet())
                            .add(score.getUser().getId());
                    affectedDifficulties.add(score.getMapDifficulty().getId());
                } else {
                    skipped++;
                }
            }

            if (i + batchSize < scores.size()) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Score correction interrupted at {}/{}", i + batchSize, scores.size());
                    break;
                }
            }

            if ((i / batchSize + 1) % 50 == 0) {
                log.info("Score correction progress: {}/{} checked, {} corrected so far",
                        Math.min(i + batchSize, scores.size()), scores.size(), corrected);
            }
        }

        log.info("Score correction phase done: {} corrected, {} unchanged, {} failed",
                corrected, skipped, failed);

        if (corrected == 0) {
            log.info("No scores were corrected - skipping recalculations");
            return;
        }

        log.info("Reassigning ranks for {} affected difficulties", affectedDifficulties.size());
        for (UUID difficultyId : affectedDifficulties) {
            scoreRankingService.reassignRanks(difficultyId);
        }

        log.info("Recalculating stats for {} categories", affectedByCategory.size());
        for (var entry : affectedByCategory.entrySet()) {
            batchRecalculateStats(entry.getValue(), entry.getKey());
            rankingService.updateRankings(entry.getKey());
        }
        overallStatisticsService.updateOverallRankings();

        log.info("Modifier score correction complete: {} corrected, {} unchanged, {} failed",
                corrected, skipped, failed);
    }

    @Transactional
    public boolean correctSingleScore(Score score) {
        Optional<BeatLeaderScoreResponse> blResponse = beatLeaderClient.getScore(score.getBlScoreId());
        if (blResponse.isEmpty()) {
            log.warn("BL score {} not found - skipping correction for score {}", score.getBlScoreId(), score.getId());
            return false;
        }

        BeatLeaderScoreResponse bl = blResponse.get();
        Integer correctBaseScore = bl.getBaseScore();

        if (Objects.equals(score.getScoreNoMods(), correctBaseScore)) {
            return false;
        }

        log.debug("Correcting score {} (BL {}): scoreNoMods {} -> {}, score {} -> recalculated",
                score.getId(), score.getBlScoreId(), score.getScoreNoMods(), correctBaseScore, score.getScore());

        List<Modifier> modifiers = modifierLinkRepository.findByScore_Id(score.getId()).stream()
                .map(ScoreModifierLink::getModifier)
                .toList();

        Integer correctedScore = applyModifierMultiplier(correctBaseScore, modifiers);

        score.setScoreNoMods(correctBaseScore);
        score.setScore(correctedScore);

        Integer maxScore = score.getMapDifficulty().getMaxScore();
        if (maxScore != null && maxScore > 0) {
            BigDecimal accuracy = BigDecimal.valueOf(correctedScore)
                    .divide(BigDecimal.valueOf(maxScore), ACCURACY_SCALE, RoundingMode.HALF_UP);
            BigDecimal complexity = mapComplexityService.findActiveComplexity(score.getMapDifficulty().getId())
                    .orElse(null);
            if (complexity != null) {
                APResult apResult = apCalculationService.calculateRawAP(
                        accuracy, complexity, score.getMapDifficulty().getCategory().getScoreCurve());
                score.setAp(apResult.rawAP());
            }
        }

        scoreRepository.save(score);
        return true;
    }

    private Integer applyModifierMultiplier(Integer baseScore, List<Modifier> modifiers) {
        if (modifiers.isEmpty())
            return baseScore;
        BigDecimal combined = modifiers.stream()
                .map(Modifier::getMultiplier)
                .reduce(BigDecimal.ONE, BigDecimal::multiply);
        return combined.multiply(BigDecimal.valueOf(baseScore))
                .setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private void batchRecalculateStats(Set<Long> userIds, UUID categoryId) {
        List<CompletableFuture<Void>> futures = userIds.stream()
                .map(userId -> CompletableFuture.runAsync(() -> {
                    try {
                        statisticsService.recalculate(userId, categoryId, false);
                    } catch (Exception e) {
                        log.error("Stats recalc failed for user {} in category {}: {}",
                                userId, categoryId, e.getMessage());
                    }
                }, backfillExecutor))
                .toList();
        futures.forEach(CompletableFuture::join);
    }
}
