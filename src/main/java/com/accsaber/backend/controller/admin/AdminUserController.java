package com.accsaber.backend.controller.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.accsaber.backend.model.dto.request.user.CountryOverrideRequest;
import com.accsaber.backend.scheduler.PlayerRefreshScheduler;
import com.accsaber.backend.service.player.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Users")
public class AdminUserController {

    private final UserService userService;
    private final PlayerRefreshScheduler playerRefreshScheduler;

    @Operation(summary = "Ban a user", description = "Bans a user, excluding them from leaderboards and rankings. Ranking recalculation runs asynchronously. Profile remains accessible.")
    @PostMapping("/{userId}/ban")
    public ResponseEntity<Void> banUser(@PathVariable Long userId) {
        userService.setBanned(userId, true);
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Unban a user", description = "Unbans a previously banned user, restoring them to leaderboards and rankings. Recalculation runs asynchronously.")
    @PostMapping("/{userId}/unban")
    public ResponseEntity<Void> unbanUser(@PathVariable Long userId) {
        userService.setBanned(userId, false);
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Override a user's country", description = "Sets a manual country override, preventing platform refreshes from changing it.")
    @PatchMapping("/{userId}/country")
    public ResponseEntity<Void> overrideCountry(@PathVariable Long userId,
            @Valid @RequestBody CountryOverrideRequest request) {
        userService.overrideCountry(userId, request.getCountry());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Clear a user's country override", description = "Removes the country override, allowing platform refreshes to update the country again.")
    @DeleteMapping("/{userId}/country-override")
    public ResponseEntity<Void> clearCountryOverride(@PathVariable Long userId) {
        userService.clearCountryOverride(userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Refresh all player profiles", description = "Triggers an async refresh of all player profiles from BeatLeader and ScoreSaber, updating names, avatars, countries, and activity status.")
    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshAllPlayers() {
        playerRefreshScheduler.refreshAllPlayersAsync();
        return ResponseEntity.accepted().build();
    }
}
