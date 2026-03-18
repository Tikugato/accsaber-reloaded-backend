package com.accsaber.backend.controller.badge;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.accsaber.backend.model.dto.response.badge.BadgeResponse;
import com.accsaber.backend.model.dto.response.badge.UserBadgeResponse;
import com.accsaber.backend.service.badge.BadgeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "Badges")
public class BadgeController {

    private final BadgeService badgeService;

    @Operation(summary = "List all active badges")
    @GetMapping("/badges")
    public ResponseEntity<List<BadgeResponse>> listBadges() {
        return ResponseEntity.ok(badgeService.findAllActive());
    }

    @Operation(summary = "Get a badge by ID")
    @GetMapping("/badges/{id}")
    public ResponseEntity<BadgeResponse> getBadge(@PathVariable UUID id) {
        return ResponseEntity.ok(badgeService.findById(id));
    }

    @Operation(summary = "Get badges for a user")
    @GetMapping("/users/{userId}/badges")
    public ResponseEntity<List<UserBadgeResponse>> getUserBadges(@PathVariable Long userId) {
        return ResponseEntity.ok(badgeService.findUserBadges(userId));
    }
}
