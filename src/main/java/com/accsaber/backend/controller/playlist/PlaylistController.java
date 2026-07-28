package com.accsaber.backend.controller.playlist;

import java.util.Map;
import java.util.Optional;

import java.util.UUID;

import com.accsaber.backend.exception.ResourceNotFoundException;
import com.accsaber.backend.exception.ValidationException;
import com.accsaber.backend.model.entity.campaign.Campaign;
import com.accsaber.backend.model.entity.map.Batch;
import com.accsaber.backend.repository.campaign.CampaignRepository;
import com.accsaber.backend.repository.map.BatchRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.accsaber.backend.service.playlist.PlaylistService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/playlists")
@RequiredArgsConstructor
@Tag(name = "Playlists")
public class PlaylistController {

        private final PlaylistService playlistService;
        private final BatchRepository batchRepository;
        private final CampaignRepository campaignRepository;

        @Operation(summary = "Download the playlist for a category", description = "Every ranked map in a category as a Beat "
                        + "Saber playlist file. Drop it in your playlists folder or hand the URL to a mod manager, and the "
                        + "syncURL inside it means it will keep itself up to date as more maps get ranked.")
        @GetMapping(value = "/{category}", produces = "application/json")
        public ResponseEntity<Map<String, Object>> getPlaylistByPath(
                        @Parameter(description = "Category code (e.g. true_acc, standard_acc, tech_acc)") @PathVariable String category) {
                return buildPlaylistResponse(category);
        }

        @Operation(summary = "Download the maps a player is missing", description = "The same idea as the category playlist but "
                        + "only the ranked maps the player has not scored on yet, so it shrinks as they work through it. Pass "
                        + "overall as the category if you want every category rather than just one.")
        @GetMapping(value = "/missing/{userId}/{category}", produces = "application/json")
        public ResponseEntity<Map<String, Object>> getMissingPlaylistByPath(
                        @Parameter(description = "User ID of the player") @PathVariable Long userId,
                        @Parameter(description = "Category code (e.g. true_acc, standard_acc, tech_acc, overall)") @PathVariable String category) {
                return buildMissingPlaylistResponse(category, userId);
        }

        @Operation(summary = "Download the queued and qualified maps", description = "Everything currently sitting in the queue "
                        + "or qualified for a category, so the maps on their way to being ranked but not there yet. Worth "
                        + "having if you like playing them before they start counting.")
        @GetMapping(value = "/unranked/{category}", produces = "application/json")
        public ResponseEntity<Map<String, Object>> getUnrankedPlaylistByPath(
                        @Parameter(description = "Category code (e.g. true_acc, standard_acc, tech_acc)") @PathVariable String category) {
                return buildUnrankedPlaylistResponse(category);
        }

        @Operation(summary = "Download a snipe playlist", description = "Every map where the target player is ahead of you, "
                        + "closest gap first, so the ones you have the best shot at taking back come up early. The playlist "
                        + "picture is the target's avatar. This form gives you all of them across every category.")
        @GetMapping(value = "/snipe/{sniperId}/{targetId}", produces = "application/json")
        public ResponseEntity<Map<String, Object>> getSnipePlaylist(
                        @Parameter(description = "User ID of the sniping player") @PathVariable Long sniperId,
                        @Parameter(description = "User ID of the target player") @PathVariable Long targetId) {
                return buildSnipePlaylistResponse(sniperId, targetId, 0, null);
        }

        @Operation(summary = "Download a snipe playlist with a size cap", description = "The same snipe playlist but stopping "
                        + "after however many maps you ask for, which keeps it manageable when the target is a long way ahead "
                        + "of you. Pass 0 if you actually want all of them.")
        @GetMapping(value = "/snipe/{sniperId}/{targetId}/{size}", produces = "application/json")
        public ResponseEntity<Map<String, Object>> getSnipePlaylistBySize(
                        @Parameter(description = "User ID of the sniping player") @PathVariable Long sniperId,
                        @Parameter(description = "User ID of the target player") @PathVariable Long targetId,
                        @Parameter(description = "Map count cap (0 = unlimited)") @PathVariable int size) {
                return buildSnipePlaylistResponse(sniperId, targetId, size, null);
        }

        @Operation(summary = "Download a snipe playlist for one category", description = "A snipe playlist narrowed to a single "
                        + "category, still capped by size. Pass 0 for size to lift the cap, and overall as the category if you "
                        + "wanted every category after all.")
        @GetMapping(value = "/snipe/{sniperId}/{targetId}/{size}/{category}", produces = "application/json")
        public ResponseEntity<Map<String, Object>> getSnipePlaylistBySizeAndCategory(
                        @Parameter(description = "User ID of the sniping player") @PathVariable Long sniperId,
                        @Parameter(description = "User ID of the target player") @PathVariable Long targetId,
                        @Parameter(description = "Map count cap (0 = unlimited)") @PathVariable int size,
                        @Parameter(description = "Category code") @PathVariable String category) {
                return buildSnipePlaylistResponse(sniperId, targetId, size, category);
        }

        @Operation(summary = "Download a batch release as a playlist", description = "All the maps that went ranked together in "
                        + "one batch. Handy right after a release when you want to play through the new set.")
        @GetMapping(value = "/batch/{batchId}", produces = "application/json")
        public ResponseEntity<Map<String, Object>> getBatchPlaylist(
                        @Parameter(description = "ID of batch") @PathVariable UUID batchId) {
                return buildBatchPlaylistResponse(batchId);
        }

        @Operation(summary = "Download a campaign as a playlist", description = "Every map used in a campaign, so you can grab "
                        + "the lot up front rather than downloading each one as you reach it. The person who made the campaign "
                        + "has to have turned playlist export on, and you get a 422 back if they have not.")
        @GetMapping(value = "/campaign/{campaignId}", produces = "application/json")
        public ResponseEntity<Map<String, Object>> getCampaignPlaylist(
                        @Parameter(description = "ID of campaign") @PathVariable UUID campaignId) {
                return buildCampaignPlaylistResponse(campaignId);
        }

        private ResponseEntity<Map<String, Object>> buildCampaignPlaylistResponse(UUID campaignId) {
                Campaign campaign = campaignRepository.findByIdAndActiveTrue(campaignId)
                                .orElseThrow(() -> new ResourceNotFoundException("Campaign", campaignId));
                if (!campaign.isPlaylistExportEnabled()) {
                        throw new ValidationException("Playlist export is not enabled for this campaign");
                }
                String syncUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                                .path("/v1/playlists/campaign/{campaignId}")
                                .buildAndExpand(campaignId)
                                .toUriString();

                Map<String, Object> playlist = playlistService.generateCampaignPlaylist(campaign, syncUrl);

                String filename = "accsaber-campaign-" + campaign.getSlug() + ".bplist";

                return ResponseEntity.ok()
                                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                                .header("Cache-Control", "no-store")
                                .body(playlist);
        }

        private ResponseEntity<Map<String, Object>> buildBatchPlaylistResponse(UUID batchId) {
                Batch batch = batchRepository.findById(batchId)
                                .orElseThrow(() -> new ResourceNotFoundException("Batch", batchId));
                String syncUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                                .path("/v1/playlists/batch/{batchId}")
                                .buildAndExpand(batchId)
                                .toUriString();

                Map<String, Object> playlist = playlistService.generateBatchPlaylist(batch, syncUrl);

                String filename = "accsaber-reloaded-"
                                + batch.getName().toLowerCase().replace(" ", "-").replace("_", "-")
                                + ".bplist";

                return ResponseEntity.ok()
                                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                                .body(playlist);
        }

        private ResponseEntity<Map<String, Object>> buildSnipePlaylistResponse(Long sniperId, Long targetId,
                        int size, String category) {
                Optional<String> categoryParam = Optional.ofNullable(category).filter(c -> !c.isBlank());
                String syncUrl = categoryParam
                                .map(c -> ServletUriComponentsBuilder.fromCurrentContextPath()
                                                .path("/v1/playlists/snipe/{sniperId}/{targetId}/{size}/{category}")
                                                .buildAndExpand(sniperId, targetId, size, c)
                                                .toUriString())
                                .orElseGet(() -> ServletUriComponentsBuilder.fromCurrentContextPath()
                                                .path("/v1/playlists/snipe/{sniperId}/{targetId}/{size}")
                                                .buildAndExpand(sniperId, targetId, size)
                                                .toUriString());
                Map<String, Object> playlist = playlistService.generateSnipePlaylist(sniperId, targetId, category, size,
                                syncUrl);

                String filenameSuffix = categoryParam.map(c -> "-" + c.replace("_", "-")).orElse("");
                String filename = "accsaber-snipe-" + sniperId + "-" + targetId + filenameSuffix + ".bplist";

                return ResponseEntity.ok()
                                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                                .header("Cache-Control", "no-store")
                                .body(playlist);
        }

        private ResponseEntity<Map<String, Object>> buildMissingPlaylistResponse(String category, Long userId) {
                String syncUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                                .path("/v1/playlists/missing/{userId}/{category}")
                                .buildAndExpand(userId, category)
                                .toUriString();
                Map<String, Object> playlist = playlistService.generateMissingPlaylist(userId, category, syncUrl);

                String filename = "accsaber-reloaded-missing-" + userId + "-"
                                + category.replace("_", "-") + ".bplist";

                return ResponseEntity.ok()
                                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                                .header("Cache-Control", "public, max-age=300")
                                .body(playlist);
        }

        private ResponseEntity<Map<String, Object>> buildPlaylistResponse(String category) {
                String syncUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                                .path("/v1/playlists/{category}")
                                .buildAndExpand(category)
                                .toUriString();
                Map<String, Object> playlist = playlistService.generatePlaylist(category, syncUrl);

                String filename = "accsaber-reloaded-" + category.replace("_", "-") + ".bplist";

                return ResponseEntity.ok()
                                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                                .body(playlist);
        }

        private ResponseEntity<Map<String, Object>> buildUnrankedPlaylistResponse(String category) {
                String syncUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                                .path("/v1/playlists/unranked/{category}")
                                .buildAndExpand(category)
                                .toUriString();
                Map<String, Object> playlist = playlistService.generateUnrankedPlaylist(category, syncUrl);

                String filename = "accsaber-reloaded-unranked-" + category.replace("_", "-") + ".bplist";

                return ResponseEntity.ok()
                                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                                .body(playlist);
        }
}
