package com.accsaber.backend.controller.stats;

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

import com.accsaber.backend.model.dto.response.statistics.CampaignCompletorResponse;
import com.accsaber.backend.model.dto.response.statistics.CampaignCreatorResponse;
import com.accsaber.backend.model.dto.response.statistics.CampaignFunnelResponse;
import com.accsaber.backend.model.dto.response.statistics.CampaignNodeDifficultyResponse;
import com.accsaber.backend.model.dto.response.statistics.TimeSeriesPointResponse;
import com.accsaber.backend.service.stats.CampaignStatisticsService;
import com.accsaber.backend.service.stats.CampaignStatsFilter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/statistics/campaigns")
@RequiredArgsConstructor
@Tag(name = "Site Statistics")
public class CampaignStatisticsController {

    private final CampaignStatisticsService campaignStatisticsService;

    @Operation(summary = "Campaign funnel", description = "Per campaign, how many people started it, how many are still "
            + "going, how many finished and how many walked away, plus the median days a finisher took. Set "
            + "minParticipants to keep out campaigns two people ever touched, since those otherwise sit at 100% or 0% and "
            + "crowd out anything real. "
            + "status takes several values at once out of published, editing, curated, loved and official, and picking "
            + "more than one widens the result, so curated and loved together means either of them. Drafts never appear. "
            + "country counts only the participants from that country, and a campaign nobody there has touched drops to "
            + "zero rather than disappearing.")
    @GetMapping("/funnel")
    public ResponseEntity<Page<CampaignFunnelResponse>> getFunnel(
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) String country,
            @RequestParam(defaultValue = "3") int minParticipants,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(campaignStatisticsService.getFunnel(
                new CampaignStatsFilter(status, country, minParticipants), pageable));
    }

    @Operation(summary = "Which nodes stop people", description = "Every node of one campaign with how many players got "
            + "it unlocked against how many actually cleared it, hardest first. Unlocked means their prerequisite chain "
            + "was done, so a node with a hundred people sitting on it and four clears is the wall the campaign runs "
            + "into. Barriers are included, since a barrier nobody gets past is the same problem.")
    @GetMapping("/hardest-nodes")
    public ResponseEntity<List<CampaignNodeDifficultyResponse>> getHardestNodes(
            @RequestParam UUID campaignId,
            @RequestParam(required = false) String country) {
        return ResponseEntity.ok(campaignStatisticsService.getNodeDifficulty(campaignId, country));
    }

    @Operation(summary = "Campaigns started per day", description = "How many people picked up a campaign on each day of "
            + "a range. Unit is h for hours, d for days, w for weeks or mo for months, and amount is how many to go back.")
    @GetMapping("/charts/starts-per-day")
    public ResponseEntity<List<TimeSeriesPointResponse>> getStartsPerDay(
            @RequestParam(defaultValue = "30") int amount,
            @RequestParam(defaultValue = "d") String unit,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) String country) {
        return ResponseEntity.ok(campaignStatisticsService.getStartsPerDay(amount, unit,
                new CampaignStatsFilter(status, country, 0)));
    }

    @Operation(summary = "Campaigns finished per day", description = "How many campaign runs were completed on each day "
            + "of a range. Unit is h for hours, d for days, w for weeks or mo for months.")
    @GetMapping("/charts/completions-per-day")
    public ResponseEntity<List<TimeSeriesPointResponse>> getCompletionsPerDay(
            @RequestParam(defaultValue = "30") int amount,
            @RequestParam(defaultValue = "d") String unit,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) String country) {
        return ResponseEntity.ok(campaignStatisticsService.getCompletionsPerDay(amount, unit,
                new CampaignStatsFilter(status, country, 0)));
    }

    @Operation(summary = "Most campaigns completed", description = "Players ranked by finished campaigns, with the nodes "
            + "they have cleared and their campaign XP alongside.")
    @GetMapping("/leaderboards/most-completed")
    public ResponseEntity<Page<CampaignCompletorResponse>> getMostCompleted(
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) String country,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(campaignStatisticsService.getMostCompleted(
                new CampaignStatsFilter(status, country, 0), pageable));
    }

    @Operation(summary = "Campaign creators by reach", description = "Whoever built the campaigns, ranked by how many "
            + "people have played them and how many got to the end. Drafts do not count.")
    @GetMapping("/leaderboards/top-creators")
    public ResponseEntity<Page<CampaignCreatorResponse>> getTopCreators(
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) String country,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(campaignStatisticsService.getTopCreators(
                new CampaignStatsFilter(status, country, 0), pageable));
    }
}
