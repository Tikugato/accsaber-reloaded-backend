package com.accsaber.backend.controller.admin;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.accsaber.backend.model.dto.request.badge.AwardBadgeRequest;
import com.accsaber.backend.model.dto.request.badge.CreateBadgeRequest;
import com.accsaber.backend.model.dto.response.badge.BadgeResponse;
import com.accsaber.backend.model.dto.response.badge.UserBadgeResponse;
import com.accsaber.backend.security.StaffUserDetails;
import com.accsaber.backend.service.badge.BadgeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/admin/badges")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Badges")
public class AdminBadgeController {

    private final BadgeService badgeService;

    @Operation(summary = "Create a badge")
    @PostMapping
    public ResponseEntity<BadgeResponse> createBadge(@Valid @RequestBody CreateBadgeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(badgeService.create(request.getName(), request.getDescription(), request.getImageUrl()));
    }

    @Operation(summary = "Deactivate a badge")
    @PatchMapping("/{id}")
    public ResponseEntity<Void> deactivateBadge(@PathVariable UUID id) {
        badgeService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Award a badge to a user")
    @PostMapping("/award")
    public ResponseEntity<UserBadgeResponse> awardBadge(
            @Valid @RequestBody AwardBadgeRequest request,
            @AuthenticationPrincipal StaffUserDetails userDetails) {
        UserBadgeResponse response = badgeService.awardBadge(
                request.getUserId(),
                request.getBadgeId(),
                userDetails.getStaffUser(),
                request.getReason());
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
