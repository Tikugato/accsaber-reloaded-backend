package com.accsaber.backend.controller.map;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.accsaber.backend.model.dto.response.map.MapComplexityHistoryResponse;
import com.accsaber.backend.model.dto.response.map.MapDifficultyStatisticsResponse;
import com.accsaber.backend.model.dto.response.map.PublicMapDifficultyResponse;
import com.accsaber.backend.model.dto.response.map.PublicMapResponse;
import com.accsaber.backend.model.dto.response.map.RankedDifficultyResponse;
import com.accsaber.backend.model.dto.response.score.ScoreResponse;
import com.accsaber.backend.model.dto.response.score.ScoresAroundResponse;
import com.accsaber.backend.exception.UnauthorizedException;
import com.accsaber.backend.model.entity.map.Difficulty;
import com.accsaber.backend.model.entity.map.MapDifficultyStatus;
import com.accsaber.backend.model.entity.user.UserRelationType;
import com.accsaber.backend.security.PlayerUserDetails;
import com.accsaber.backend.service.infra.CategoryService;
import com.accsaber.backend.service.map.MapDifficultyStatisticsService;
import com.accsaber.backend.service.map.MapService;
import com.accsaber.backend.service.player.UserRelationService;
import com.accsaber.backend.service.score.ScoreService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/v1/maps")
@RequiredArgsConstructor
@Tag(name = "Maps")
public class MapController {

    private final MapService mapService;
    private final ScoreService scoreService;
    private final MapDifficultyStatisticsService statisticsService;
    private final UserRelationService userRelationService;
    private final CategoryService categoryService;

    @Operation(summary = "List maps", description = "A page of maps. You can narrow it down by category, passing either the UUID "
            + "or the code like true_acc, by status, or with a search term that looks across the song name, the song author and "
            + "the mapper at the same time. Bear in mind a map here means the song itself, and the individual difficulties hang "
            + "off it.")
    @GetMapping
    public ResponseEntity<Page<PublicMapResponse>> listMaps(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) MapDifficultyStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "songName", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity
                .ok(mapService.findAllPublic(categoryService.resolveId(categoryId), status, search, pageable));
    }

    @Operation(summary = "List difficulties", description = "A page of difficulties with their map details attached. This is "
            + "usually the one you want rather than the map list, since ranked status, category and complexity all live on the "
            + "difficulty rather than on the song. Filter by category, status, a complexity range or a search term. Status takes "
            + "more than one value if you need it, like status=QUEUE,QUALIFIED.")
    @GetMapping("/difficulties")
    public ResponseEntity<Page<PublicMapDifficultyResponse>> listDifficulties(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) List<MapDifficultyStatus> status,
            @RequestParam(required = false) BigDecimal complexityMin,
            @RequestParam(required = false) BigDecimal complexityMax,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "rankedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity
                .ok(mapService.findDifficultiesPublic(categoryService.resolveId(categoryId), status, complexityMin,
                        complexityMax, search, null, pageable));
    }

    @Operation(summary = "All ranked difficulties", description = "Every ranked difficulty in one flat list, with the song hash, "
            + "difficulty level, current complexity and when it went ranked. It stays cached until the ranked set actually "
            + "changes, so it is cheap to call and it is the right way to sync a local copy rather than paging through the "
            + "difficulty list.")
    @GetMapping("/difficulties/all")
    public ResponseEntity<List<RankedDifficultyResponse>> getAllRankedDifficulties() {
        return ResponseEntity.ok(mapService.findAllRankedDifficulties());
    }

    @Operation(summary = "Get one difficulty", description = "A single difficulty by id. Complexity only comes back once it is "
            + "ranked, and vote counts and criteria status only come back while it is not, since neither of those means anything "
            + "in the other state.")
    @GetMapping("/difficulties/{difficultyId}")
    public ResponseEntity<PublicMapDifficultyResponse> getDifficulty(@PathVariable UUID difficultyId) {
        return ResponseEntity.ok(mapService.getDifficultyResponsePublic(difficultyId));
    }

    @Operation(summary = "Get one map", description = "A single map by id, with all of its active difficulties attached.")
    @GetMapping("/{mapId}")
    public ResponseEntity<PublicMapResponse> getMap(@PathVariable UUID mapId) {
        return ResponseEntity.ok(mapService.findByIdPublic(mapId));
    }

    @Operation(summary = "Get a map by song hash", description = "Look a map up by its song hash, which is what you will have "
            + "if you are working from a local file. You get the active difficulties back with it, and you can pass a difficulty "
            + "to narrow it to one level. Those are EASY, NORMAL, HARD, EXPERT and EXPERT_PLUS.")
    @GetMapping("/hash/{songHash}")
    public ResponseEntity<PublicMapResponse> getMapBySongHash(
            @PathVariable String songHash,
            @RequestParam(required = false) Difficulty difficulty) {
        return ResponseEntity.ok(mapService.findBySongHashPublic(songHash, difficulty));
    }

    @Operation(summary = "Get a map by BeatSaver code", description = "Look a map up by its BeatSaver code, the short one out of "
            + "the map page URL. You can narrow it with a difficulty, a characteristic, or both. Characteristic is case "
            + "insensitive, so Standard, OneSaber, NoArrows, 90Degree, 360Degree, Lightshow and Lawless all work however you "
            + "happen to capitalise them.")
    @GetMapping("/by-code/{beatsaverCode}")
    public ResponseEntity<PublicMapResponse> getMapByBeatsaverCode(
            @PathVariable String beatsaverCode,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) String characteristic) {
        return ResponseEntity.ok(mapService.findByBeatsaverCodePublic(beatsaverCode, difficulty, characteristic));
    }

    @Operation(summary = "List the difficulties on a map", description = "All the active difficulties belonging to one map.")
    @GetMapping("/{mapId}/difficulties")
    public ResponseEntity<List<PublicMapDifficultyResponse>> listMapDifficulties(@PathVariable UUID mapId) {
        return ResponseEntity.ok(mapService.findDifficultiesByMapIdPublic(mapId));
    }

    @Operation(summary = "Get a leaderboard by platform leaderboard ID", description = "The same leaderboard you get from the "
            + "difficulty route below, except you look it up with a BeatLeader or ScoreSaber leaderboard ID instead of our own "
            + "difficulty id. Pass exactly one of the two. This is the one to reach for when you are coming from the platform "
            + "side and do not know our ids yet. All the same filters apply.")
    @GetMapping("/difficulties/leaderboard/{leaderboardId}/scores")
    public ResponseEntity<Page<ScoreResponse>> getDifficultyScoresByLeaderboardId(
            @PathVariable String leaderboardId,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserRelationType relation,
            @AuthenticationPrincipal PlayerUserDetails principal,
            @PageableDefault(size = 20, sort = "score", direction = Sort.Direction.DESC) Pageable pageable) {
        UUID difficultyId = mapService.findDifficultyIdByLeaderboardId(leaderboardId);
        java.util.Collection<Long> filter = resolveRelationFilter(relation, principal);
        return ResponseEntity.ok(
                scoreService.findLeaderboardByMapDifficulty(difficultyId, country, search, filter, pageable));
    }

    @Operation(summary = "Get the leaderboard for a difficulty", description = "Every score set on one map difficulty with the "
            + "player attached, ordered from the highest score down. If you want a smaller slice you can filter by country code "
            + "like ES or GB, search by player name, or pass a relation type to only see people you follow. That last filter "
            + "needs a logged in player token, the rest work fine without one. maxStreak115 counts every attempt the player "
            + "has made here, not just the score on the row, and you can sort by it.")
    @GetMapping("/difficulties/{difficultyId}/scores")
    public ResponseEntity<Page<ScoreResponse>> getDifficultyLeaderboard(
            @PathVariable UUID difficultyId,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserRelationType relation,
            @AuthenticationPrincipal PlayerUserDetails principal,
            @PageableDefault(size = 20, sort = "score", direction = Sort.Direction.DESC) Pageable pageable) {
        java.util.Collection<Long> filter = resolveRelationFilter(relation, principal);
        return ResponseEntity.ok(
                scoreService.findLeaderboardByMapDifficulty(difficultyId, country, search, filter, pageable));
    }

    private java.util.Collection<Long> resolveRelationFilter(UserRelationType relation, PlayerUserDetails principal) {
        if (relation == null) {
            return null;
        }
        if (principal == null) {
            throw new UnauthorizedException("Player authentication required to filter by relation");
        }
        return userRelationService.findRelationFilterUserIds(principal.getUserId(), relation);
    }

    @Operation(summary = "Scores around a player", description = "The slice of a difficulty leaderboard sitting either side of "
            + "one player, which is what you want for a your position view. You get four above and five below by default, and if "
            + "there are not enough on one side the remainder moves over to the other so you always come away with the same "
            + "number of rows. Looked up by BeatLeader or ScoreSaber leaderboard ID.")
    @GetMapping("/difficulties/leaderboard/{leaderboardId}/scores-around/{userId}")
    public ResponseEntity<ScoresAroundResponse> getScoresAround(
            @PathVariable String leaderboardId,
            @PathVariable Long userId,
            @RequestParam(defaultValue = "4") int above,
            @RequestParam(defaultValue = "5") int below) {
        UUID difficultyId = mapService.findDifficultyIdByLeaderboardId(leaderboardId);
        return ResponseEntity.ok(scoreService.findScoresAround(difficultyId, userId, above, below));
    }

    @Operation(summary = "Current statistics for a difficulty", description = "The aggregate numbers as they stand right now, so "
            + "the highest and lowest AP anyone has managed on it, the average, and how many scores there are altogether. You "
            + "get a 204 with nothing in it if nobody has scored on the difficulty yet.")
    @GetMapping("/difficulties/{difficultyId}/statistics")
    public ResponseEntity<MapDifficultyStatisticsResponse> getDifficultyStatistics(
            @PathVariable UUID difficultyId) {
        return statisticsService.findActive(difficultyId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(summary = "Historic statistics for a difficulty", description = "The same aggregate numbers but every version of "
            + "them across a time range, oldest first, so you can chart how a difficulty has moved. Unit is h for hours, d for "
            + "days, w for weeks or mo for months, and amount is how many of those to go back.")
    @GetMapping("/difficulties/{difficultyId}/statistics/historic")
    public ResponseEntity<List<MapDifficultyStatisticsResponse>> getDifficultyStatisticsHistoric(
            @PathVariable UUID difficultyId,
            @RequestParam(defaultValue = "7") int amount,
            @RequestParam(defaultValue = "d") String unit) {
        return ResponseEntity.ok(statisticsService.findHistoric(difficultyId, amount, unit));
    }

    @Operation(summary = "Complexity history for a map", description = "Every complexity the map has been given over time, along "
            + "with who changed it and the reason they gave. Complexity feeds straight into AP, so each entry here means every "
            + "score on that difficulty was recalculated at the time.")
    @GetMapping("/{mapId}/complexity-history")
    public ResponseEntity<List<MapComplexityHistoryResponse>> getComplexityHistory(@PathVariable UUID mapId) {
        return ResponseEntity.ok(mapService.getComplexityHistory(mapId));
    }
}
