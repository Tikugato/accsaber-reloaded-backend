package com.accsaber.backend.service.score;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.accsaber.backend.repository.score.ScoreRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreRankingService {

    private final ScoreRepository scoreRepository;

    @Transactional
    public void reassignRanks(UUID difficultyId) {
        scoreRepository.reassignScoreRanks(difficultyId);
    }

    @Transactional
    public void reassignRanksForBackfill(UUID difficultyId) {
        scoreRepository.reassignScoreRanks(difficultyId);
        scoreRepository.syncRankWhenSetFromRank(difficultyId);
    }

    public int rankNewScore(UUID difficultyId, BigDecimal ap, Instant timeSet) {
        int rank = scoreRepository.countActiveScoresRankedAbove(difficultyId, ap, timeSet) + 1;
        scoreRepository.shiftScoreRanksDown(difficultyId, rank);
        return rank;
    }

    public int rankImprovedScore(UUID difficultyId, int oldRank, BigDecimal newAp, Instant timeSet) {
        scoreRepository.shiftScoreRanksUp(difficultyId, oldRank);
        int rank = scoreRepository.countActiveScoresRankedAbove(difficultyId, newAp, timeSet) + 1;
        scoreRepository.shiftScoreRanksDown(difficultyId, rank);
        return rank;
    }

    @Transactional
    public void reassignAllRanks() {
        List<UUID> difficultyIds = scoreRepository.findDistinctActiveDifficultyIds();
        if (difficultyIds.isEmpty()) {
            log.info("Score rank repair: no difficulties with active scores found");
            return;
        }
        log.info("Score rank repair: reassigning ranks for {} difficulties", difficultyIds.size());
        for (UUID difficultyId : difficultyIds) {
            scoreRepository.reassignScoreRanks(difficultyId);
        }
        log.info("Score rank repair: complete");
    }
}
