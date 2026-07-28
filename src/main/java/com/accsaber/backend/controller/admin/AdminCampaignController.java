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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.accsaber.backend.model.dto.request.campaign.CreateCampaignRequest;
import com.accsaber.backend.model.dto.request.campaign.CreateCampaignTagRequest;
import com.accsaber.backend.model.dto.response.campaign.CampaignResponse;
import com.accsaber.backend.model.dto.response.campaign.CampaignTagResponse;
import com.accsaber.backend.security.StaffUserDetails;
import com.accsaber.backend.service.campaign.CampaignService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/admin/campaigns")
@PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_CURATOR')")
@RequiredArgsConstructor
@Tag(name = "Admin - Campaigns")
public class AdminCampaignController {

    private final CampaignService campaignService;

    @Operation(summary = "Create a campaign")
    @PostMapping
    public ResponseEntity<CampaignResponse> createCampaign(@Valid @RequestBody CreateCampaignRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(campaignService.createCampaign(request));
    }

    @Operation(summary = "Move a published campaign back into editing")
    @PatchMapping("/{campaignId}/edit")
    public ResponseEntity<CampaignResponse> startEditing(@PathVariable UUID campaignId) {
        return ResponseEntity.ok(campaignService.startEditing(campaignId));
    }

    @Operation(summary = "Mark a campaign as curated")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_CURATOR')")
    @PatchMapping("/{campaignId}/curate")
    public ResponseEntity<CampaignResponse> markCurated(
            @PathVariable UUID campaignId,
            @AuthenticationPrincipal StaffUserDetails principal) {
        return ResponseEntity.ok(campaignService.markCurated(campaignId, principal.getStaffUser()));
    }

    @Operation(summary = "Mark a campaign as loved by the community")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_CURATOR')")
    @PatchMapping("/{campaignId}/loved")
    public ResponseEntity<CampaignResponse> setLoved(
            @PathVariable UUID campaignId,
            @RequestParam(name = "loved", defaultValue = "true") boolean loved,
            @AuthenticationPrincipal StaffUserDetails principal) {
        return ResponseEntity.ok(campaignService.setLoved(campaignId, loved, principal.getStaffUser()));
    }

    @Operation(summary = "Strip curation status from a campaign")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_CURATOR')")
    @PatchMapping("/{campaignId}/uncurate")
    public ResponseEntity<CampaignResponse> uncurate(
            @PathVariable UUID campaignId,
            @AuthenticationPrincipal StaffUserDetails principal) {
        return ResponseEntity.ok(campaignService.uncurate(campaignId, principal.getStaffUser()));
    }

    @Operation(summary = "Mark a campaign official (allows its creators to reward untradeable items)")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{campaignId}/official")
    public ResponseEntity<CampaignResponse> setOfficial(
            @PathVariable UUID campaignId,
            @RequestParam(name = "value", defaultValue = "true") boolean official) {
        return ResponseEntity.ok(campaignService.setOfficial(campaignId, official));
    }

    @Operation(summary = "Create a campaign tag")
    @PostMapping("/tags")
    public ResponseEntity<CampaignTagResponse> createTag(
            @Valid @RequestBody CreateCampaignTagRequest request,
            @AuthenticationPrincipal StaffUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(campaignService.createTag(request, principal != null ? principal.getStaffUser() : null));
    }
}
