package com.accsaber.backend.controller.stats;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.accsaber.backend.model.dto.response.statistics.DistributionEntryResponse;
import com.accsaber.backend.model.dto.response.statistics.MissionCalibrationResponse;
import com.accsaber.backend.model.dto.response.statistics.MissionCompletorResponse;
import com.accsaber.backend.model.dto.response.statistics.MissionShortfallResponse;
import com.accsaber.backend.model.dto.response.statistics.MissionXpResponse;
import com.accsaber.backend.model.dto.response.statistics.TimeSeriesPointResponse;
import com.accsaber.backend.model.entity.mission.MissionBand;
import com.accsaber.backend.model.entity.mission.MissionPool;
import com.accsaber.backend.model.entity.mission.MissionType;
import com.accsaber.backend.service.infra.CategoryService;
import com.accsaber.backend.service.stats.MissionShortfallService;
import com.accsaber.backend.service.stats.MissionStatisticsService;
import com.accsaber.backend.service.stats.MissionStatsFilter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/statistics/missions")
@RequiredArgsConstructor
@Tag(name = "Site Statistics")
public class MissionStatisticsController {

    private final MissionStatisticsService missionStatisticsService;
    private final MissionShortfallService missionShortfallService;
    private final CategoryService categoryService;

    @Operation(summary = "Mission calibration table", description = "One row per mission template, band and skill tier, "
            + "with how many were handed out, how many got finished, and the rate that falls out of the two. A mission "
            + "nobody in a tier ever finishes is either too hard for that tier or asking for something they cannot reach. "
            + "Missions thrown away by staff rather than failed by the player do not count toward the total. Progressed "
            + "counts the ones the player actually put a dent in, and is empty for the one-shot types since those bank no "
            + "partial progress.\n\n"
            + "Every list filter takes several values at once, so pool=daily&pool=weekly widens the result rather than "
            + "narrowing it. skillMin and skillMax cut on the raw skill threshold the target was built from, which is the "
            + "same number the tiers bucket, so use those for a range that does not line up with a tier boundary. Set "
            + "minAssigned to keep out combinations only a handful of people ever saw.")
    @GetMapping("/calibration")
    public ResponseEntity<Page<MissionCalibrationResponse>> getCalibration(
            @RequestParam(required = false) List<MissionPool> pool,
            @RequestParam(required = false) List<MissionType> type,
            @RequestParam(required = false) UUID templateId,
            @RequestParam(required = false) List<String> categoryId,
            @RequestParam(required = false) List<MissionBand> band,
            @RequestParam(required = false) List<String> tier,
            @RequestParam(required = false) Double skillMin,
            @RequestParam(required = false) Double skillMax,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "1") int minAssigned,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(missionStatisticsService.getCalibration(
                filter(pool, type, templateId, categoryId, band, tier, skillMin, skillMax, country, from, to,
                        minAssigned),
                pageable));
    }

    @Operation(summary = "One mission across the skill tiers", description = "The completion rate for a single template "
            + "broken out by player tier, from new through elite. Reading down the list tells you whether a mission gets "
            + "harder or easier as players climb, which is usually where a bad target shows itself.")
    @GetMapping("/calibration/by-tier")
    public ResponseEntity<List<MissionCalibrationResponse>> getByTier(
            @RequestParam UUID templateId,
            @RequestParam(required = false) List<MissionPool> pool,
            @RequestParam(required = false) List<String> categoryId,
            @RequestParam(required = false) List<MissionBand> band,
            @RequestParam(required = false) Double skillMin,
            @RequestParam(required = false) Double skillMax,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        return ResponseEntity.ok(missionStatisticsService.getByTier(templateId,
                filter(pool, null, null, categoryId, band, null, skillMin, skillMax, country, from, to, 1)));
    }

    @Operation(summary = "What each mission pays out", description = "XP handed over per template and band, with the "
            + "total, the average, the median and the 90th percentile, plus what share of all mission XP that template "
            + "accounts for. This is where you find the ones paying far more than the work they ask for.")
    @GetMapping("/xp")
    public ResponseEntity<Page<MissionXpResponse>> getXpPayouts(
            @RequestParam(required = false) List<MissionPool> pool,
            @RequestParam(required = false) List<MissionType> type,
            @RequestParam(required = false) UUID templateId,
            @RequestParam(required = false) List<String> categoryId,
            @RequestParam(required = false) List<MissionBand> band,
            @RequestParam(required = false) List<String> tier,
            @RequestParam(required = false) Double skillMin,
            @RequestParam(required = false) Double skillMax,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(missionStatisticsService.getXpPayouts(
                filter(pool, type, templateId, categoryId, band, tier, skillMin, skillMax, country, from, to, 1),
                pageable));
    }

    @Operation(summary = "How close the failures got", description = "For one template, how far along players were when "
            + "the mission ran out, as a fraction of the target. Counting missions use the progress they banked. One-shot "
            + "missions bank nothing, so they get measured against the player's best score on the target map instead, "
            + "which shows whether the target was ever reachable for them. A pile of failures sitting at 95% means the "
            + "target is slightly too high; a pile sitting at 40% means it was never on.")
    @GetMapping("/shortfall")
    public ResponseEntity<List<MissionShortfallResponse>> getShortfall(
            @RequestParam UUID templateId,
            @RequestParam(required = false) List<MissionBand> band,
            @RequestParam(required = false) List<String> tier,
            @RequestParam(required = false) List<String> categoryId,
            @RequestParam(required = false) Double skillMin,
            @RequestParam(required = false) Double skillMax,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        return ResponseEntity.ok(missionShortfallService.getShortfall(templateId,
                filter(null, null, null, categoryId, band, tier, skillMin, skillMax, country, from, to, 1)));
    }

    @Operation(summary = "Completion rate over time", description = "The share of missions that got finished, per day, "
            + "so you can see whether a change to a template moved anything. Comes back as a whole percentage rather "
            + "than a fraction. Unit is h for hours, d for days, w for weeks or mo for months, and amount is how many to "
            + "go back. Anything over 65 days rolls up to weekly points.")
    @GetMapping("/charts/completion-rate")
    public ResponseEntity<List<TimeSeriesPointResponse>> getCompletionRate(
            @RequestParam(defaultValue = "30") int amount,
            @RequestParam(defaultValue = "d") String unit,
            @RequestParam(required = false) List<MissionPool> pool,
            @RequestParam(required = false) List<MissionType> type,
            @RequestParam(required = false) UUID templateId,
            @RequestParam(required = false) List<MissionBand> band,
            @RequestParam(required = false) List<String> tier,
            @RequestParam(required = false) String country) {
        return ResponseEntity.ok(missionStatisticsService.getCompletionRateOverTime(amount, unit,
                filter(pool, type, templateId, null, band, tier, null, null, country, null, null, 1)));
    }

    @Operation(summary = "Missions completed per day", description = "How many missions got finished on each day of a "
            + "range. Unit is h for hours, d for days, w for weeks or mo for months, and amount is how many to go back.")
    @GetMapping("/charts/completions-per-day")
    public ResponseEntity<List<TimeSeriesPointResponse>> getCompletionsPerDay(
            @RequestParam(defaultValue = "30") int amount,
            @RequestParam(defaultValue = "d") String unit,
            @RequestParam(required = false) List<MissionPool> pool,
            @RequestParam(required = false) List<MissionType> type,
            @RequestParam(required = false) List<String> tier,
            @RequestParam(required = false) String country) {
        return ResponseEntity.ok(missionStatisticsService.getCompletionsPerDay(amount, unit,
                filter(pool, type, null, null, null, tier, null, null, country, null, null, 1)));
    }

    @Operation(summary = "Completions by mission type", description = "How finished missions split across the types, for "
            + "a pie or a bar chart.")
    @GetMapping("/charts/by-type")
    public ResponseEntity<List<DistributionEntryResponse>> getCompletionsByType(
            @RequestParam(required = false) List<MissionPool> pool,
            @RequestParam(required = false) List<String> tier,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        return ResponseEntity.ok(missionStatisticsService.getCompletionsByType(
                filter(pool, null, null, null, null, tier, null, null, country, from, to, 1)));
    }

    @Operation(summary = "Most missions completed", description = "Players ranked by how many missions they have "
            + "finished. Narrow by pool, type, tier or country.")
    @GetMapping("/leaderboards/most-completed")
    public ResponseEntity<Page<MissionCompletorResponse>> getMostCompleted(
            @RequestParam(required = false) List<MissionPool> pool,
            @RequestParam(required = false) List<MissionType> type,
            @RequestParam(required = false) List<String> tier,
            @RequestParam(required = false) String country,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(missionStatisticsService.getMostCompleted(
                filter(pool, type, null, null, null, tier, null, null, country, null, null, 1), pageable));
    }

    @Operation(summary = "Most XP earned from missions", description = "Players ranked by lifetime mission XP, which "
            + "rewards finishing the ones that pay rather than finishing the most.")
    @GetMapping("/leaderboards/most-mission-xp")
    public ResponseEntity<Page<MissionCompletorResponse>> getMostMissionXp(
            @RequestParam(required = false) String country,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(missionStatisticsService.getMostMissionXp(country, pageable));
    }

    private MissionStatsFilter filter(List<MissionPool> pools, List<MissionType> types, UUID templateId,
            List<String> categoryIds, List<MissionBand> bands, List<String> tiers, Double skillMin, Double skillMax,
            String country, Instant from, Instant to, int minAssigned) {
        List<UUID> resolvedCategories = categoryIds == null ? null
                : categoryIds.stream().map(categoryService::resolveId).filter(java.util.Objects::nonNull).toList();
        return new MissionStatsFilter(pools, types, templateId, resolvedCategories, bands, tiers,
                skillMin, skillMax, country, from, to, minAssigned);
    }
}
