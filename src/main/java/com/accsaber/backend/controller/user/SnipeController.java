package com.accsaber.backend.controller.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.accsaber.backend.model.dto.response.score.SnipeComparisonResponse;
import com.accsaber.backend.service.snipe.SnipeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Tag(name = "Players")
public class SnipeController {

    private static final int MAX_PAGE_SIZE = 100;

    private final SnipeService snipeService;

    @Operation(summary = "Find where you are closest to catching someone", description = "The difficulties where a target "
            + "player is ahead of the sniper, smallest gap first, so the ones worth going after come up first. Each row "
            + "carries both players' current scores so you can show the comparison without a second call. Pass category to "
            + "narrow it. This is the data behind the snipe playlists, if you want the same thing as a downloadable file "
            + "instead.")
    @GetMapping("/{sniperId}/closest-to/{targetId}")
    public ResponseEntity<Page<SnipeComparisonResponse>> getClosestScores(
            @Parameter(description = "User ID of the sniping player") @PathVariable Long sniperId,
            @Parameter(description = "User ID of the target player") @PathVariable Long targetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Optional category code; omit for all categories") @RequestParam(required = false) String category) {
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
        return ResponseEntity.ok(snipeService.findClosestScores(sniperId, targetId, category, pageable));
    }
}
