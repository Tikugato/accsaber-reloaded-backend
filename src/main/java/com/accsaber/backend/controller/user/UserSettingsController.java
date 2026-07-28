package com.accsaber.backend.controller.user;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.accsaber.backend.exception.UnauthorizedException;
import com.accsaber.backend.security.PlayerUserDetails;
import com.accsaber.backend.service.player.UserSettingsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Tag(name = "Players")
public class UserSettingsController {

    private final UserSettingsService settingsService;

    @Operation(summary = "Get all your settings", description = "Every setting for whoever the token belongs to, across all "
            + "groups. Anything the player has never touched comes back as its default rather than being left out, so you can "
            + "render a settings screen straight from this without filling in gaps yourself.")
    @GetMapping("/me/settings")
    public ResponseEntity<Map<String, Object>> getMyAllSettings(
            @AuthenticationPrincipal PlayerUserDetails principal) {
        return ResponseEntity.ok(settingsService.getAll(requirePrincipal(principal).getUserId()));
    }

    @Operation(summary = "Get one of your settings groups", description = "The same thing narrowed to a single group, so privacy "
            + "or appearance and so on. Defaults get filled in here too.")
    @GetMapping("/me/settings/{group}")
    public ResponseEntity<Map<String, Object>> getMyGroup(
            @PathVariable String group,
            @AuthenticationPrincipal PlayerUserDetails principal) {
        return ResponseEntity.ok(settingsService.getGroup(requirePrincipal(principal).getUserId(), group));
    }

    @Operation(summary = "Change one of your settings groups", description = "Send a partial map of just the keys you want to "
            + "change and the rest stay as they were, so you do not need to read the group first and send it all back. You get "
            + "the whole group back afterwards with your changes applied.")
    @PutMapping("/me/settings/{group}")
    public ResponseEntity<Map<String, Object>> patchMyGroup(
            @PathVariable String group,
            @RequestBody Map<String, Object> patch,
            @AuthenticationPrincipal PlayerUserDetails principal) {
        return ResponseEntity.ok(settingsService.updateGroup(requirePrincipal(principal).getUserId(), group, patch));
    }

    @Operation(summary = "Get another player's public settings", description = "The settings a player has agreed to make "
            + "readable, which is only the keys marked as public in the registry rather than everything in the group. Worth "
            + "checking before you go after data that might be gated, since it tells you what they have chosen to share.")
    @GetMapping("/{userId}/settings/{group}")
    public ResponseEntity<Map<String, Object>> getPublicGroup(
            @PathVariable Long userId,
            @PathVariable String group) {
        return ResponseEntity.ok(settingsService.getPublicGroup(userId, group));
    }

    private PlayerUserDetails requirePrincipal(PlayerUserDetails principal) {
        if (principal == null) {
            throw new UnauthorizedException("Player authentication required");
        }
        return principal;
    }
}
