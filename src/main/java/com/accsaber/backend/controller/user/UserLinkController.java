package com.accsaber.backend.controller.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.accsaber.backend.model.dto.response.player.UserResponse;
import com.accsaber.backend.service.player.DuplicateUserService;
import com.accsaber.backend.service.player.UserService;
import com.accsaber.backend.service.stats.StatisticsService;
import com.accsaber.backend.util.ProfileUrlResolver;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Tag(name = "Players")
public class UserLinkController {

    private final UserService userService;
    private final StatisticsService statisticsService;
    private final DuplicateUserService duplicateUserService;
    private final ProfileUrlResolver profileUrlResolver;

    @Operation(summary = "Find a player from a profile link", description = "Give it a BeatLeader or ScoreSaber profile URL, or "
            + "just the numeric id out of one, and it works out which player that is. Handy when someone pastes a link and you "
            + "want the AccSaber profile behind it. If that account has been merged into another, you get the primary one "
            + "back rather than a dead end. Pass statistics=true to bring the category stats along too.")
    @GetMapping("/link")
    public ResponseEntity<UserResponse> getUserByLink(
            @RequestParam String url,
            @RequestParam(defaultValue = "false") boolean statistics) {
        String platformId = profileUrlResolver.resolve(url);
        Long userId = duplicateUserService.resolvePrimaryUserId(Long.parseLong(platformId));
        UserResponse user = userService.findByUserId(userId);
        if (statistics) {
            user = user.withStatistics(statisticsService.findCategoryStatsByUser(userId));
        }
        return ResponseEntity.ok(user);
    }
}
