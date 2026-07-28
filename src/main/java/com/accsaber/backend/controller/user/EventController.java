package com.accsaber.backend.controller.user;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.accsaber.backend.exception.UnauthorizedException;
import com.accsaber.backend.model.dto.response.mission.EventDetailResponse;
import com.accsaber.backend.model.dto.response.mission.EventProgressResponse;
import com.accsaber.backend.model.dto.response.mission.MissionResponse;
import com.accsaber.backend.model.dto.response.mission.EventResponse;
import com.accsaber.backend.security.PlayerUserDetails;
import com.accsaber.backend.service.mission.EventMissionService;
import com.accsaber.backend.service.mission.EventService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/events")
@RequiredArgsConstructor
@Tag(name = "Missions and Events")
public class EventController {

    private final EventService eventService;
    private final EventMissionService eventMissionService;

    @Operation(summary = "List the events", description = "Seasonal events, past and present. Pass state to narrow it to live, "
            + "upcoming or past.")
    @GetMapping
    public ResponseEntity<List<EventResponse>> list(@RequestParam(required = false) String state) {
        return ResponseEntity.ok(eventService.listPublic(state).stream()
                .map(EventResponse::from).toList());
    }

    @Operation(summary = "Get the event running right now",
            description = "The one event that is live, as a single object rather than a list, which saves you picking it out "
                    + "of the list yourself. You get a 204 with nothing in it when there is no event on.")
    @GetMapping("/current")
    public ResponseEntity<EventResponse> current() {
        return eventService.findCurrent()
                .map(event -> ResponseEntity.ok(EventResponse.from(event)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(summary = "List an event's missions",
            description = "Every mission in an event, without anyone's progress attached. Address the event by id or by slug, "
                    + "either works. Pass week to get just one week, counting from 1, where week 1 is the first seven days "
                    + "after the event started.")
    @GetMapping("/{idOrSlug}/missions")
    public ResponseEntity<List<MissionResponse>> missions(@PathVariable String idOrSlug,
            @RequestParam(required = false) Integer week) {
        return ResponseEntity.ok(eventMissionService.getMissions(eventService.resolveId(idOrSlug), week));
    }

    @Operation(summary = "List an event's missions with your progress",
            description = "The same list but with how far you have got on each one. If any unlocked missions had not been "
                    + "handed to you yet, calling this assigns them on the spot, so it is safe to poll rather than needing "
                    + "something to have run beforehand. Id or slug both work.")
    @GetMapping("/{idOrSlug}/missions/me")
    public ResponseEntity<List<EventProgressResponse.EventMissionProgressResponse>> myMissions(
            @AuthenticationPrincipal PlayerUserDetails principal,
            @PathVariable String idOrSlug,
            @RequestParam(required = false) Integer week) {
        if (principal == null) {
            throw new UnauthorizedException("Player authentication required");
        }
        UUID id = eventService.resolveId(idOrSlug);
        Long userId = principal.getUserId();
        eventMissionService.ensureForUserAndEventId(userId, id);
        return ResponseEntity.ok(eventMissionService.getMissionsWithProgress(userId, id, week));
    }

    @Operation(summary = "Join an event",
            description = "Events are opt in, so nothing happens for a player until they call this. It sets them up and hands "
                    + "them the first week of missions. Later weeks stay shut until the current one is finished, so you cannot "
                    + "jump ahead. Calling it more than once is fine and will not reset anything.")
    @PostMapping("/{idOrSlug}/begin")
    public ResponseEntity<EventProgressResponse> begin(
            @AuthenticationPrincipal PlayerUserDetails principal,
            @PathVariable String idOrSlug) {
        if (principal == null) {
            throw new UnauthorizedException("Player authentication required");
        }
        return ResponseEntity.ok(eventMissionService.begin(principal.getUserId(), eventService.resolveId(idOrSlug)));
    }

    @Operation(summary = "Get one event", description = "An event with its missions attached, by id or by slug. This is the "
            + "public view with nobody's progress on it.")
    @GetMapping("/{idOrSlug}")
    public ResponseEntity<EventDetailResponse> get(@PathVariable String idOrSlug) {
        return ResponseEntity.ok(eventMissionService.getDetail(eventService.resolveId(idOrSlug)));
    }

    @Operation(summary = "Get an event with your progress",
            description = "The event plus where you are in it, which week you are on and what you have finished. Like the "
                    + "missions route, this hands you any unlocked missions you were missing rather than leaving gaps.")
    @GetMapping("/{idOrSlug}/me")
    public ResponseEntity<EventProgressResponse> getMyProgress(
            @AuthenticationPrincipal PlayerUserDetails principal,
            @PathVariable String idOrSlug) {
        if (principal == null) {
            throw new UnauthorizedException("Player authentication required");
        }
        UUID id = eventService.resolveId(idOrSlug);
        Long userId = principal.getUserId();
        eventMissionService.ensureForUserAndEventId(userId, id);
        return ResponseEntity.ok(eventMissionService.getProgress(userId, id));
    }
}
