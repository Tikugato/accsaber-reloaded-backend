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

import com.accsaber.backend.model.dto.response.score.ScoreResponse;
import com.accsaber.backend.model.dto.response.statistics.BiggestTraderResponse;
import com.accsaber.backend.model.dto.response.statistics.CollectionCompletionResponse;
import com.accsaber.backend.model.dto.response.statistics.DistributionEntryResponse;
import com.accsaber.backend.model.dto.response.statistics.EssenceEarnedResponse;
import com.accsaber.backend.model.dto.response.statistics.FirstEditionHolderResponse;
import com.accsaber.backend.model.dto.response.statistics.FirstEditionsResponse;
import com.accsaber.backend.model.dto.response.statistics.InventoryValueResponse;
import com.accsaber.backend.model.dto.response.statistics.ItemScarcityResponse;
import com.accsaber.backend.model.dto.response.statistics.MapAvgApResponse;
import com.accsaber.backend.model.dto.response.statistics.MapRetryResponse;
import com.accsaber.backend.model.dto.response.statistics.MilestoneCollectorResponse;
import com.accsaber.backend.model.dto.response.statistics.MostCratesOpenedResponse;
import com.accsaber.backend.model.dto.response.statistics.MostItemsResponse;
import com.accsaber.backend.model.dto.response.statistics.RarestUnboxedResponse;
import com.accsaber.backend.model.dto.response.statistics.TimeSeriesPointResponse;
import com.accsaber.backend.model.dto.response.statistics.UserImprovementsResponse;
import com.accsaber.backend.model.dto.response.statistics.UserMapImprovementsResponse;
import com.accsaber.backend.service.infra.CategoryService;
import com.accsaber.backend.service.stats.SiteStatisticsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/statistics")
@RequiredArgsConstructor
@Tag(name = "Site Statistics")
public class SiteStatisticsController {

    private final SiteStatisticsService siteStatisticsService;
    private final CategoryService categoryService;

    @Operation(summary = "Longest 115 streaks", description = "Scores ranked by the longest run of 115 notes hit in a row, which says more about "
            + "consistency than raw accuracy does. Only scores that came to us from BeatLeader can appear here, since "
            + "ScoreSaber does not give us the streak. Every attempt counts, not just your current best on a map, and "
            + "you appear once per map difficulty with your longest run on it. Narrow by category or country if you want.")
    @GetMapping("/leaderboards/streaks")
    public ResponseEntity<Page<ScoreResponse>> getTopStreaks(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String country,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity
                .ok(siteStatisticsService.getTopStreaks(categoryService.resolveId(categoryId), country, pageable));
    }

    @Operation(summary = "Highest AP scores", description = "The single best scores on the site by AP. Narrow by category or country.")
    @GetMapping("/leaderboards/max-ap")
    public ResponseEntity<Page<ScoreResponse>> getTopByAp(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String country,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity
                .ok(siteStatisticsService.getTopByAp(categoryService.resolveId(categoryId), country, pageable));
    }

    @Operation(summary = "Maps with the highest average AP", description = "Difficulties ranked by the average weighted AP people actually manage "
            + "on them. Set a minimum score count to keep out difficulties only a handful of people have touched, which "
            + "otherwise dominate the top.")
    @GetMapping("/leaderboards/highest-avg-ap")
    public ResponseEntity<Page<MapAvgApResponse>> getHighestAvgAp(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String country,
            @RequestParam(defaultValue = "5") int minScores,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(siteStatisticsService.getHighestAvgAp(categoryService.resolveId(categoryId), country,
                minScores, pageable));
    }

    @Operation(summary = "Most retried maps", description = "Difficulties people keep going back to beat their own score on. It counts superseded "
            + "scores, so it measures how much a map pulls people back rather than how many played it once.")
    @GetMapping("/leaderboards/most-retried")
    public ResponseEntity<Page<MapRetryResponse>> getMostRetriedMaps(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String country,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity
                .ok(siteStatisticsService.getMostRetriedMaps(categoryService.resolveId(categoryId), country, pageable));
    }

    @Operation(summary = "Players who improve the most", description = "Ranked by how many times someone has beaten their own score anywhere on "
            + "the site, so it rewards grinding rather than raw skill.")
    @GetMapping("/leaderboards/most-improvements")
    public ResponseEntity<Page<UserImprovementsResponse>> getMostImprovements(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String country,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                siteStatisticsService.getMostImprovements(categoryService.resolveId(categoryId), country, pageable));
    }

    @Operation(summary = "Most persistent on one map", description = "Ranked by the most times anyone has beaten their own score on a single "
            + "difficulty. Essentially the leaderboard of refusing to let a map go.")
    @GetMapping("/leaderboards/most-map-improvements")
    public ResponseEntity<Page<UserMapImprovementsResponse>> getMostMapImprovements(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String country,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                siteStatisticsService.getMostMapImprovements(categoryService.resolveId(categoryId), country, pageable));
    }

    @Operation(summary = "Milestone collectors", description = "Players ranked by how many milestones they have finished.")
    @GetMapping("/leaderboards/milestone-collectors")
    public ResponseEntity<Page<MilestoneCollectorResponse>> getMilestoneCollectors(
            @RequestParam(required = false) String country,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(siteStatisticsService.getMilestoneCollectors(country, pageable));
    }

    @Operation(summary = "Biggest collections", description = "Players ranked by how many items they are holding. Only tradeable ones count "
            + "toward this, so untradeable items never show up here however many someone has. Narrow by item type, modifier "
            + "or country.")
    @GetMapping("/leaderboards/most-items")
    public ResponseEntity<Page<MostItemsResponse>> getMostItems(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String modifier,
            @RequestParam(required = false) String country,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(siteStatisticsService.getMostItems(type, modifier, country, pageable));
    }

    @Operation(summary = "Most crates opened", description = "Players ranked by how many crates they have got through. Pass a crate item id to "
            + "scope it to one kind of crate.")
    @GetMapping("/leaderboards/most-crates-opened")
    public ResponseEntity<Page<MostCratesOpenedResponse>> getMostCratesOpened(
            @RequestParam(required = false) UUID crateId,
            @RequestParam(required = false) String country,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(siteStatisticsService.getMostCratesOpened(crateId, country, pageable));
    }

    @Operation(summary = "Luckiest crate pulls", description = "Individual items that came out of crates, ranked by how many modifiers they "
            + "landed and then by rarity. This is the wall of people getting very fortunate.")
    @GetMapping("/leaderboards/rarest-unboxed")
    public ResponseEntity<Page<RarestUnboxedResponse>> getRarestUnboxed(
            @RequestParam(required = false) String country,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(siteStatisticsService.getRarestUnboxed(country, pageable));
    }

    @Operation(summary = "Most valuable inventories", description = "Players ranked by what their tradeable items would come to if they "
            + "disintegrated the lot, plus whatever essence they are already sitting on.")
    @GetMapping("/leaderboards/most-valuable-inventory")
    public ResponseEntity<Page<InventoryValueResponse>> getMostValuableInventory(
            @RequestParam(required = false) String country,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(siteStatisticsService.getMostValuableInventory(country, pageable));
    }

    @Operation(summary = "Most first editions", description = "Players ranked by how many serial number one items they hold.")
    @GetMapping("/leaderboards/first-editions")
    public ResponseEntity<Page<FirstEditionsResponse>> getFirstEditions(
            @RequestParam(required = false) String country,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(siteStatisticsService.getFirstEditions(country, pageable));
    }

    @Operation(summary = "Who holds each first edition", description = "For every tradeable item, whoever ended up with serial number one of "
            + "it. The other way round from the list above.")
    @GetMapping("/leaderboards/first-edition-holders")
    public ResponseEntity<Page<FirstEditionHolderResponse>> getFirstEditionHolders(
            @RequestParam(required = false) String country,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(siteStatisticsService.getFirstEditionHolders(country, pageable));
    }

    @Operation(summary = "Most complete collections", description = "Players ranked by how much of the tradeable catalogue they own, as a "
            + "percentage rather than a raw count, so it does not simply reward whoever has been here longest.")
    @GetMapping("/leaderboards/most-complete-collection")
    public ResponseEntity<Page<CollectionCompletionResponse>> getMostCompleteCollection(
            @RequestParam(required = false) String country,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(siteStatisticsService.getMostCompleteCollection(country, pageable));
    }

    @Operation(summary = "Rarest items", description = "Catalogue items ranked by how few copies are actually out there. You get both the number "
            + "of copies and the number of distinct owners, since one person holding twenty is a rather different kind of "
            + "rare from twenty people holding one each.")
    @GetMapping("/leaderboards/rarest-items")
    public ResponseEntity<Page<ItemScarcityResponse>> getItemScarcity(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(siteStatisticsService.getItemScarcity(pageable));
    }

    @Operation(summary = "Busiest traders", description = "Players ranked by how many trades they have actually completed. Offers that were never "
            + "accepted do not count.")
    @GetMapping("/leaderboards/biggest-traders")
    public ResponseEntity<Page<BiggestTraderResponse>> getBiggestTraders(
            @RequestParam(required = false) String country,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(siteStatisticsService.getBiggestTraders(country, pageable));
    }

    @Operation(summary = "Most essence earned", description = "Players ranked by how much essence they have pulled out of disintegrating things. "
            + "This is lifetime earned rather than what they are holding, so somebody can top this and have nothing left.")
    @GetMapping("/leaderboards/most-essence-earned")
    public ResponseEntity<Page<EssenceEarnedResponse>> getMostEssenceEarned(
            @RequestParam(required = false) String country,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(siteStatisticsService.getMostEssenceEarned(country, pageable));
    }

    @Operation(summary = "New players per day", description = "How many people joined on each day of a range. "
            + "Unit is h for hours, d for days, w for weeks or mo for months, and amount is how many to go back. Anything over "
            + "65 days gets rolled up to weekly points so the response stays a sensible size. Narrow by country if you want.")
    @GetMapping("/charts/new-players-per-day")
    public ResponseEntity<List<TimeSeriesPointResponse>> getNewPlayersPerDay(
            @RequestParam(defaultValue = "30") int amount,
            @RequestParam(defaultValue = "d") String unit,
            @RequestParam(required = false) String country) {
        return ResponseEntity.ok(siteStatisticsService.getNewPlayersPerDay(amount, unit, country));
    }

    @Operation(summary = "Scores per day", description = "How many scores came in on each day of a range. "
            + "Unit is h for hours, d for days, w for weeks or mo for months, and amount is how many to go back. Anything over "
            + "65 days gets rolled up to weekly points so the response stays a sensible size. Narrow by country if you want.")
    @GetMapping("/charts/scores-per-day")
    public ResponseEntity<List<TimeSeriesPointResponse>> getScoresPerDay(
            @RequestParam(defaultValue = "30") int amount,
            @RequestParam(defaultValue = "d") String unit,
            @RequestParam(required = false) String country) {
        return ResponseEntity.ok(siteStatisticsService.getScoresPerDay(amount, unit, country));
    }

    @Operation(summary = "Accounts over time", description = "A running total of active accounts, so the line only ever climbs. "
            + "Unit is h for hours, d for days, w for weeks or mo for months, and amount is how many to go back. Anything over "
            + "65 days gets rolled up to weekly points so the response stays a sensible size. Narrow by country if you want.")
    @GetMapping("/charts/cumulative-accounts")
    public ResponseEntity<List<TimeSeriesPointResponse>> getCumulativeAccounts(
            @RequestParam(defaultValue = "30") int amount,
            @RequestParam(defaultValue = "d") String unit,
            @RequestParam(required = false) String country) {
        return ResponseEntity.ok(siteStatisticsService.getCumulativeAccounts(amount, unit, country));
    }

    @Operation(summary = "Scores over time", description = "A running total of every score we hold. "
            + "Unit is h for hours, d for days, w for weeks or mo for months, and amount is how many to go back. Anything over "
            + "65 days gets rolled up to weekly points so the response stays a sensible size. Narrow by country if you want.")
    @GetMapping("/charts/cumulative-scores")
    public ResponseEntity<List<TimeSeriesPointResponse>> getCumulativeScores(
            @RequestParam(defaultValue = "30") int amount,
            @RequestParam(defaultValue = "d") String unit,
            @RequestParam(required = false) String country) {
        return ResponseEntity.ok(siteStatisticsService.getCumulativeScores(amount, unit, country));
    }

    @Operation(summary = "Scores by category", description = "How the scores split across the categories, for a pie or a bar chart.")
    @GetMapping("/charts/scores-per-category")
    public ResponseEntity<List<DistributionEntryResponse>> getScoresPerCategory(
            @RequestParam(required = false) String country) {
        return ResponseEntity.ok(siteStatisticsService.getScoresPerCategory(country));
    }

    @Operation(summary = "Players by headset", description = "How players split across headset models. We take it from whatever they were "
            + "using on their most recent score, so it follows people when they upgrade rather than sticking to whatever "
            + "they started on.")
    @GetMapping("/charts/players-by-hmd")
    public ResponseEntity<List<DistributionEntryResponse>> getPlayersByHmd(
            @RequestParam(required = false) String country) {
        return ResponseEntity.ok(siteStatisticsService.getPlayersByHmd(country));
    }

    @Operation(summary = "Players by country", description = "How the active player base splits across countries.")
    @GetMapping("/charts/players-per-country")
    public ResponseEntity<List<DistributionEntryResponse>> getPlayersPerCountry() {
        return ResponseEntity.ok(siteStatisticsService.getPlayersPerCountry());
    }
}
