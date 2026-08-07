package com.accsaber.backend.model.dto.response.mission;

import com.accsaber.backend.util.Rounding;

import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.accsaber.backend.model.dto.EventMissionTargets;
import com.accsaber.backend.model.dto.response.item.ItemResponse;
import com.accsaber.backend.model.entity.Category;
import com.accsaber.backend.model.entity.map.MapDifficulty;
import com.accsaber.backend.model.entity.mission.Event;
import com.accsaber.backend.model.entity.mission.MissionTemplate;
import com.accsaber.backend.model.entity.mission.MissionType;
import com.accsaber.backend.model.entity.mission.UserMission;
import com.accsaber.backend.model.entity.user.User;
import com.accsaber.backend.service.item.ItemMapper;
import com.accsaber.backend.util.MissionDescriptionRenderer;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MissionResponse {

    private UUID id;
    private String code;
    private String name;
    private String description;
    private String type;
    private String pool;
    private String status;
    private String band;

    private UUID categoryId;
    private String categoryCode;

    private UUID targetMapDifficultyId;
    private String targetMapSongName;
    private String targetPlayerId;
    private String targetPlayerName;

    private Double targetAcc;
    private Double targetAp;
    private Integer targetScore;
    private Integer targetCount;
    private Integer targetXp;
    private Double targetThresholdAp;
    private Integer targetStreak;
    private Instant targetRankedBefore;
    private Boolean targetCuratedOnly;

    private Integer progressCount;
    private Double progressAp;
    private Double progressValue;
    private Double targetValue;
    private Integer xpReward;
    private ItemResponse itemReward;

    private Instant assignedAt;
    private Instant expiresAt;
    private Instant completedAt;

    private Instant unlocksAt;
    private Instant completableUntil;
    private Integer week;
    private Boolean unlocked;
    private Boolean open;
    private Boolean repeatable;
    private Integer maxCompletions;

    public static MissionResponse from(UserMission m) {
        return MissionResponse.builder()
                .id(m.getId())
                .name(m.getTemplate().getName())
                .description(renderDescription(m))
                .type(m.getTemplate().getType().name())
                .pool(m.getPool().name())
                .status(m.getStatus().name())
                .band(m.getBand() != null ? m.getBand().name() : null)
                .categoryId(m.getCategory() != null ? m.getCategory().getId() : null)
                .categoryCode(m.getCategory() != null ? m.getCategory().getCode() : null)
                .targetMapDifficultyId(m.getTargetMapDifficulty() != null
                        ? m.getTargetMapDifficulty().getId() : null)
                .targetMapSongName(m.getTargetMapDifficulty() != null
                        && m.getTargetMapDifficulty().getMap() != null
                                ? m.getTargetMapDifficulty().getMap().getSongName() : null)
                .targetPlayerId(m.getTargetPlayer() != null
                        ? String.valueOf(m.getTargetPlayer().getId()) : null)
                .targetPlayerName(m.getTargetPlayer() != null
                        ? m.getTargetPlayer().getName() : null)
                .targetAcc(roundAcc(m.getTargetAcc()))
                .targetAp(roundAp(m.getTargetAp()))
                .targetScore(m.getTargetScore())
                .targetCount(countTarget(m.getTemplate().getType(), m.getTargetCount(), m.getTargetAp()))
                .targetXp(m.getTargetXp())
                .targetThresholdAp(roundAp(m.getTargetThresholdAp()))
                .targetStreak(m.getTargetStreak())
                .targetRankedBefore(m.getTargetRankedBefore())
                .targetCuratedOnly(m.getTargetCuratedOnly())
                .progressCount(countProgress(m.getTemplate().getType(), m.getProgressCount(), m.getProgressAp()))
                .progressAp(roundProgressAp(m.getProgressAp()))
                .progressValue(progressValue(m.getTemplate().getType(), m.getProgressCount(), m.getProgressAp()))
                .targetValue(targetValue(m.getTemplate().getType(), m.getTargetCount(), m.getTargetXp(),
                        m.getTargetAp()))
                .xpReward(m.getXpReward())
                .itemReward(m.getItemReward() != null ? ItemMapper.toItemResponse(m.getItemReward()) : null)
                .assignedAt(m.getAssignedAt())
                .expiresAt(m.getExpiresAt())
                .completedAt(m.getCompletedAt())
                .build();
    }

    public static MissionResponse fromTemplate(MissionTemplate t, Event event, Instant now, TargetContext ctx) {
        Instant unlocksAt = t.unlockInstant(event);
        Instant until = t.closeInstant(event);
        EventMissionTargets targets = t.getEventTargets();
        Category category = ctx.category(targets);
        MapDifficulty mapDifficulty = ctx.mapDifficulty(targets);
        User player = ctx.player(targets);
        return MissionResponse.builder()
                .id(t.getId())
                .code(t.getCode())
                .name(t.getName())
                .description(renderTemplateDescription(t, targets, category, mapDifficulty, player))
                .type(t.getType().name())
                .pool(t.getPool().name())
                .categoryId(category != null ? category.getId() : null)
                .categoryCode(category != null ? category.getCode() : null)
                .targetMapDifficultyId(mapDifficulty != null ? mapDifficulty.getId() : null)
                .targetMapSongName(mapDifficulty != null && mapDifficulty.getMap() != null
                        ? mapDifficulty.getMap().getSongName() : null)
                .targetPlayerId(player != null ? String.valueOf(player.getId()) : null)
                .targetPlayerName(player != null ? player.getName() : null)
                .targetAcc(targets != null ? roundAcc(targets.acc()) : null)
                .targetAp(targets != null ? roundAp(targets.ap()) : null)
                .targetScore(targets != null ? targets.score() : null)
                .targetCount(targets != null
                        ? countTarget(t.getType(), targets.count(), targets.ap()) : null)
                .targetXp(targets != null ? targets.xp() : null)
                .targetThresholdAp(targets != null ? roundAp(targets.thresholdAp()) : null)
                .targetStreak(targets != null ? targets.streak() : null)
                .targetRankedBefore(targets != null ? targets.rankedBefore() : null)
                .targetCuratedOnly(targets != null ? targets.curatedOnly() : null)
                .targetValue(targets == null ? null
                        : targetValue(t.getType(), targets.count(), targets.xp(), targets.ap()))
                .xpReward(t.getFixedXp())
                .itemReward(t.getAwardsItem() != null ? ItemMapper.toItemResponse(t.getAwardsItem()) : null)
                .unlocksAt(unlocksAt)
                .completableUntil(until)
                .week(t.weekOf(event))
                .unlocked(!unlocksAt.isAfter(now))
                .open(t.isOpenAt(event, now))
                .repeatable(t.isRepeatable())
                .maxCompletions(t.getMaxCompletions())
                .build();
    }

    private static Integer countTarget(MissionType type, Integer targetCount, Double targetAp) {
        if (type != MissionType.AP_GAIN_OVERALL || targetCount != null || targetAp == null) {
            return targetCount;
        }
        return (int) Math.ceil(targetAp);
    }

    private static Integer countProgress(MissionType type, Integer progressCount, Double progressAp) {
        if (type != MissionType.AP_GAIN_OVERALL || progressAp == null) {
            return progressCount;
        }
        return (int) Math.floor(progressAp);
    }

    private static Double targetValue(MissionType type, Integer targetCount, Integer targetXp,
            Double targetAp) {
        return switch (type) {
            case AP_GAIN_OVERALL -> roundAp(targetAp);
            case XP_IN_WINDOW -> targetXp == null ? null : (double) (targetXp);
            case PLAY_N_MAPS, SCORES_N, STREAK_N_IN_CATEGORY, STREAK_SUM_N, PB_ABOVE_THRESHOLD,
                    SNIPE_RIVAL_ANY_MAP, BATCH_PLAY_N, PB_RANKED_BEFORE_N, CAMPAIGN_COMPLETE_N ->
                targetCount == null ? null : (double) (targetCount);
            case ACC_ON_MAP, AP_ON_MAP, PB_SPECIFIC_MAP, COMEBACK_PB, SNIPE_PLAYER_ON_MAP, STREAK_ON_MAP -> null;
        };
    }

    private static Double progressValue(MissionType type, Integer progressCount, Double progressAp) {
        return switch (type) {
            case AP_GAIN_OVERALL -> roundProgressAp(progressAp);
            case XP_IN_WINDOW, PLAY_N_MAPS, SCORES_N, STREAK_N_IN_CATEGORY, STREAK_SUM_N, PB_ABOVE_THRESHOLD,
                    SNIPE_RIVAL_ANY_MAP, BATCH_PLAY_N, PB_RANKED_BEFORE_N, CAMPAIGN_COMPLETE_N ->
                progressCount == null ? null : (double) (progressCount);
            case ACC_ON_MAP, AP_ON_MAP, PB_SPECIFIC_MAP, COMEBACK_PB, SNIPE_PLAYER_ON_MAP, STREAK_ON_MAP -> null;
        };
    }

    private static Double roundAcc(Double acc) {
        return acc == null ? null :Rounding.round((acc * (double) (100)), 2);
    }

    private static Double roundAp(Double ap) {
        return ap == null ? null : Rounding.round(ap, 0);
    }

    private static Double roundProgressAp(Double ap) {
        return ap == null ? null : Rounding.round(ap, 2);
    }

    public static String renderDescription(UserMission m) {
        return MissionDescriptionRenderer.render(m.getTemplate().getDescription(),
                new MissionDescriptionRenderer.Values(
                        m.getTargetCount(), m.getTargetXp(), m.getTargetAcc(), m.getTargetAp(),
                        m.getTargetScore(), m.getTargetThresholdAp(), m.getTargetStreak(),
                        MissionDescriptionRenderer.formatMap(m.getTargetMapDifficulty()),
                        m.getTargetPlayer() != null ? m.getTargetPlayer().getName() : null,
                        m.getCategory() != null ? m.getCategory().getName() : null));
    }

    private static String renderTemplateDescription(MissionTemplate t, EventMissionTargets targets,
            Category category, MapDifficulty mapDifficulty, User player) {
        return MissionDescriptionRenderer.render(t.getDescription(),
                new MissionDescriptionRenderer.Values(
                        targets != null ? targets.count() : null,
                        targets != null ? targets.xp() : null,
                        targets != null ? targets.acc() : null,
                        targets != null ? targets.ap() : null,
                        targets != null ? targets.score() : null,
                        targets != null ? targets.thresholdAp() : null,
                        targets != null ? targets.streak() : null,
                        MissionDescriptionRenderer.formatMap(mapDifficulty),
                        player != null ? player.getName() : null,
                        category != null ? category.getName() : null));
    }

    public record TargetContext(
            Map<UUID, Category> categories,
            Map<UUID, MapDifficulty> mapDifficulties,
            Map<Long, User> players) {

        public Category category(EventMissionTargets targets) {
            return targets != null && targets.categoryId() != null
                    ? categories.get(targets.categoryId())
                    : null;
        }

        public MapDifficulty mapDifficulty(EventMissionTargets targets) {
            return targets != null && targets.mapDifficultyId() != null
                    ? mapDifficulties.get(targets.mapDifficultyId())
                    : null;
        }

        public User player(EventMissionTargets targets) {
            return targets != null && targets.playerId() != null
                    ? players.get(targets.playerIdAsLong())
                    : null;
        }
    }
}
