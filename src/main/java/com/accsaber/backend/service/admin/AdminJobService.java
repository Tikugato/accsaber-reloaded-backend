package com.accsaber.backend.service.admin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;

import com.accsaber.backend.exception.ValidationException;
import com.accsaber.backend.model.dto.request.admin.RunJobRequest;
import com.accsaber.backend.model.entity.map.MapDifficulty;
import com.accsaber.backend.repository.map.MapDifficultyRepository;
import com.accsaber.backend.service.media.CdnSyncService;
import com.accsaber.backend.service.milestone.MilestoneService;
import com.accsaber.backend.service.score.ScoreImportService;
import com.accsaber.backend.service.score.ScoreIngestionService;
import com.accsaber.backend.service.score.ScoreRecalculationService;
import com.accsaber.backend.service.score.XPReweightService;
import com.accsaber.backend.service.songsuggest.SongSuggestService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminJobService {

    private final JobRegistry registry;
    private final ScoreRecalculationService scoreRecalculationService;
    private final XPReweightService xpReweightService;
    private final ScoreImportService scoreImportService;
    private final ScoreIngestionService scoreIngestionService;
    private final CdnSyncService cdnSyncService;
    private final MilestoneService milestoneService;
    private final SongSuggestService songSuggestService;
    private final MapDifficultyRepository mapDifficultyRepository;

    public JobRecord run(RunJobRequest request) {
        JobRecord job = registry.start(request.getType(), describe(request));
        CompletableFuture<Void> work;
        try {
            work = dispatch(request);
        } catch (RuntimeException e) {
            registry.fail(job.id(), e);
            throw e;
        }
        work.whenComplete((ignored, error) -> {
            if (error != null) {
                registry.fail(job.id(), error);
            } else {
                registry.succeed(job.id());
            }
        });
        return job;
    }

    public List<JobRecord> list() {
        return registry.list();
    }

    public Optional<JobRecord> find(UUID jobId) {
        return registry.find(jobId);
    }

    private CompletableFuture<Void> dispatch(RunJobRequest request) {
        return switch (request.getType()) {
            case RECALCULATE_AP_DIFFICULTY ->
                scoreRecalculationService.recalculateDifficultyAsync(requireDifficultyId(request));
            case RECALCULATE_AP_DIFFICULTIES ->
                scoreRecalculationService.recalculateBatchAsync(loadDifficulties(request));
            case RECALCULATE_AP_RAW -> scoreRecalculationService.recalculateAllRawApAsync();
            case RECALCULATE_AP_WEIGHTED -> scoreRecalculationService.recalculateAllWeightedApAsync();
            case RECALCULATE_AP_ALL -> scoreRecalculationService.recalculateAllApAsync();
            case RECALCULATE_XP_SCORES -> xpReweightService.reweightAllScores();
            case RECALCULATE_XP_TOTALS -> xpReweightService.recalculateTotalXpForAllUsers();

            case BACKFILL_SCORES_ALL -> scoreImportService.backfillAllRankedDifficulties();
            case BACKFILL_SCORES_DIFFICULTY ->
                scoreImportService.backfillDifficultyAsync(requireDifficultyId(request));
            case BACKFILL_SCORES_DIFFICULTIES ->
                scoreImportService.backfillDifficultiesAsync(requireDifficultyIds(request));
            case BACKFILL_SCORES_USER -> scoreImportService.backfillUserAsync(requireUserId(request));
            case BACKFILL_SCORES_USERS -> scoreImportService.backfillUsersAsync(requireUserIds(request));
            case BACKFILL_SCORES_GAP_FILL ->
                scoreIngestionService.gapFillSince(requireSince(request), request.getPlatform());
            case BACKFILL_CAMPAIGN_LEGACY ->
                scoreImportService.recheckLegacyCampaign(requireCampaignId(request), request.getUserId());

            case BACKFILL_CDN_MAP_COVERS -> cdnSyncService.backfillAllMapCovers(request.isForce());
            case BACKFILL_CDN_AVATARS -> cdnSyncService.backfillAllUserAvatars(request.isForce());

            case BACKFILL_MILESTONE -> milestoneService.backfillMilestone(requireMilestoneId(request));
            case BACKFILL_MILESTONES_ALL -> milestoneService.backfillAllMilestones();
            case BACKFILL_MILESTONES_USER -> milestoneService.backfillUser(requireUserId(request));

            case REGENERATE_SONG_SUGGEST -> songSuggestService.regenerateAsync();
        };
    }

    private String describe(RunJobRequest request) {
        if (request.getDifficultyId() != null) {
            return "difficulty " + request.getDifficultyId();
        }
        if (request.getCampaignId() != null) {
            return request.getUserId() != null
                    ? "campaign " + request.getCampaignId() + " user " + request.getUserId()
                    : "campaign " + request.getCampaignId();
        }
        if (request.getUserId() != null) {
            return "user " + request.getUserId();
        }
        if (request.getDifficultyIds() != null && !request.getDifficultyIds().isEmpty()) {
            return request.getDifficultyIds().size() + " difficulties";
        }
        if (request.getUserIds() != null && !request.getUserIds().isEmpty()) {
            return request.getUserIds().size() + " users";
        }
        if (request.getSince() != null) {
            return "since " + request.getSince();
        }
        return null;
    }

    private UUID requireDifficultyId(RunJobRequest request) {
        if (request.getDifficultyId() == null) {
            throw new ValidationException("difficultyId", "is required for " + request.getType());
        }
        return request.getDifficultyId();
    }

    private List<UUID> requireDifficultyIds(RunJobRequest request) {
        if (request.getDifficultyIds() == null || request.getDifficultyIds().isEmpty()) {
            throw new ValidationException("difficultyIds", "is required for " + request.getType());
        }
        return request.getDifficultyIds();
    }

    private UUID requireMilestoneId(RunJobRequest request) {
        if (request.getMilestoneId() == null) {
            throw new ValidationException("milestoneId", "is required for " + request.getType());
        }
        return request.getMilestoneId();
    }

    private UUID requireCampaignId(RunJobRequest request) {
        if (request.getCampaignId() == null) {
            throw new ValidationException("campaignId", "is required for " + request.getType());
        }
        return request.getCampaignId();
    }

    private Long requireUserId(RunJobRequest request) {
        if (request.getUserId() == null) {
            throw new ValidationException("userId", "is required for " + request.getType());
        }
        return request.getUserId();
    }

    private List<Long> requireUserIds(RunJobRequest request) {
        if (request.getUserIds() == null || request.getUserIds().isEmpty()) {
            throw new ValidationException("userIds", "is required for " + request.getType());
        }
        return request.getUserIds();
    }

    private java.time.Instant requireSince(RunJobRequest request) {
        if (request.getSince() == null) {
            throw new ValidationException("since", "is required for " + request.getType());
        }
        return request.getSince();
    }

    private List<MapDifficulty> loadDifficulties(RunJobRequest request) {
        return mapDifficultyRepository.findAllByIdInAndActiveTrueWithCategory(requireDifficultyIds(request));
    }
}
