package com.accsaber.backend.model.dto.request.admin;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.accsaber.backend.model.entity.map.LeaderboardPlatform;
import com.accsaber.backend.service.admin.JobType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RunJobRequest {

    @Schema(description = "Which job to run. The other fields are only read by the types that need them.")
    @NotNull
    private JobType type;

    @Schema(description = "Required by RECALCULATE_AP_DIFFICULTY and BACKFILL_SCORES_DIFFICULTY.")
    private UUID difficultyId;

    @Schema(description = "Required by RECALCULATE_AP_DIFFICULTIES and BACKFILL_SCORES_DIFFICULTIES.")
    private List<UUID> difficultyIds;

    @Schema(description = "Required by BACKFILL_MILESTONE.")
    private UUID milestoneId;

    @Schema(description = "Required by BACKFILL_SCORES_USER and BACKFILL_MILESTONES_USER.")
    private Long userId;

    @Schema(description = "Required by BACKFILL_SCORES_USERS.")
    private List<Long> userIds;

    @Schema(description = "Required by BACKFILL_SCORES_GAP_FILL. Scores set after this point are pulled again.")
    private Instant since;

    @Schema(description = "Optional for BACKFILL_SCORES_GAP_FILL. Leave it off to cover both platforms.")
    private LeaderboardPlatform platform;

    @Schema(description = "Used by the CDN backfills. Pass true to re-mirror files that already look fine.")
    private boolean force;
}
