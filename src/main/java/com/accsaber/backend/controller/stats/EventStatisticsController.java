package com.accsaber.backend.controller.stats;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.accsaber.backend.model.dto.response.statistics.EventMissionLeaderboardResponse;
import com.accsaber.backend.model.dto.response.statistics.EventParticipationResponse;
import com.accsaber.backend.model.dto.response.statistics.EventSummaryResponse;
import com.accsaber.backend.service.mission.EventService;
import com.accsaber.backend.service.stats.EventStatisticsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/statistics/events")
@RequiredArgsConstructor
@Tag(name = "Site Statistics")
public class EventStatisticsController {

    private final EventStatisticsService eventStatisticsService;
    private final EventService eventService;

    @Operation(summary = "How one event went", description = "Everything about a single event in one response: how many "
            + "people took part, how many saw it through, the median number of missions they finished, how many claimed "
            + "the completion bonus, where people stopped week by week, and the completion rate on every mission in it. "
            + "Address the event by UUID or by slug. Works on events that already finished, since event missions are kept "
            + "rather than cleared out.\n\n"
            + "Counting is per player, not per row, which is what makes a repeatable mission read honestly: Marathon "
            + "handing out 230 clears to 69 people out of 187 who had it is a 36.9% mission, not a 66% one. players is "
            + "how many had it, playersCompleted how many cleared it at least once, and completions the raw number of "
            + "clears. For a mission nobody can repeat those last two are the same number. "
            + "Pass week to cut the mission list to the ones that opened in that week, numbered from 1. totalWeeks says "
            + "how far that goes, and every mission carries its own week so the unfiltered list groups without a second "
            + "call. The weeks array always covers the whole event, since that is the drop-off curve. country narrows "
            + "every number to players from there.")
    @GetMapping("/{idOrSlug}/summary")
    public ResponseEntity<EventSummaryResponse> getSummary(
            @PathVariable String idOrSlug,
            @RequestParam(required = false) Integer week,
            @RequestParam(required = false) String country) {
        return ResponseEntity.ok(eventStatisticsService.getSummary(
                eventService.resolveId(idOrSlug), week, country));
    }

    @Operation(summary = "Who ran a mission the most", description = "Players ranked by how many times they finished "
            + "a mission in this event, which only tells you anything for a repeatable one. Leave templateId off to rank "
            + "across every mission in the event instead. Ties share a rank and are broken by who got there first.")
    @GetMapping("/{idOrSlug}/missions/leaderboard")
    public ResponseEntity<Page<EventMissionLeaderboardResponse>> getMissionLeaderboard(
            @PathVariable String idOrSlug,
            @RequestParam(required = false) UUID templateId,
            @RequestParam(required = false) String country,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(eventStatisticsService.getMissionLeaderboard(
                eventService.resolveId(idOrSlug), templateId, country, pageable));
    }

    @Operation(summary = "Every event side by side", description = "Participants, finishers and missions completed for "
            + "each event, newest first, so you can see whether the last one pulled better than the one before it. Pass "
            + "eventId one or more times to compare a chosen few, and country to scope the counts to one place.")
    @GetMapping("/participation")
    public ResponseEntity<Page<EventParticipationResponse>> getParticipation(
            @RequestParam(required = false) List<String> eventId,
            @RequestParam(required = false) String country,
            @PageableDefault(size = 20) Pageable pageable) {
        List<UUID> resolved = eventId == null ? null : eventId.stream().map(eventService::resolveId).toList();
        return ResponseEntity.ok(eventStatisticsService.getParticipation(resolved, country, pageable));
    }
}
