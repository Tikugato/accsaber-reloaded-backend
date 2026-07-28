package com.accsaber.backend.controller;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.accsaber.backend.model.entity.map.Difficulty;
import com.accsaber.backend.service.og.OgService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/og")
@RequiredArgsConstructor
@Tag(name = "Platform")
public class OgController {

    private final OgService ogService;

    @Operation(summary = "Link preview for a player", description = "A small HTML page carrying the Open Graph tags for a "
            + "player, so a link to their profile unfurls nicely in Discord or anywhere else that reads them. This gives you "
            + "markup rather than JSON, and it is meant for link scrapers rather than for your own code.")
    @GetMapping(value = "/players/{userId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> playerOg(@PathVariable Long userId) {
        return ResponseEntity.ok(ogService.buildPlayerOg(userId));
    }

    @Operation(summary = "Link preview for a map", description = "The same idea for a map, addressed by id or by BeatSaver "
            + "code. Pass a difficulty id, or a difficulty and characteristic, to have the preview describe one difficulty "
            + "rather than the song as a whole.")
    @GetMapping(value = "/maps/{mapIdOrCode}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> mapOg(
            @PathVariable String mapIdOrCode,
            @RequestParam(required = false) UUID difficultyId,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) String characteristic) {
        return ResponseEntity.ok(ogService.buildMapOg(mapIdOrCode, difficultyId, difficulty, characteristic));
    }

    @Operation(summary = "Link preview for a campaign", description = "The same again for a campaign, by id or by slug.")
    @GetMapping(value = "/campaigns/{campaignIdOrSlug}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> campaignOg(@PathVariable String campaignIdOrSlug) {
        return ResponseEntity.ok(ogService.buildCampaignOg(campaignIdOrSlug));
    }
}
