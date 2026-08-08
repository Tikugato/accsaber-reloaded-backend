package com.accsaber.backend.controller.quest;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.accsaber.backend.exception.UnauthorizedException;
import com.accsaber.backend.model.dto.response.quest.QuestReleaseResponse;
import com.accsaber.backend.security.PlayerUserDetails;
import com.accsaber.backend.service.quest.QuestModService;
import com.accsaber.backend.service.quest.QuestModService.GeneratedMod;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/quest")
@RequiredArgsConstructor
@Tag(name = "Quest")
public class QuestModController {

    private final QuestModService questModService;

    @Operation(summary = "List Quest mod releases", description = "The published versions of the AccSaber Quest mod, pulled "
            + "from GitHub releases and cached for a few minutes. Each entry carries the Beat Saber version it was built "
            + "for, so the site can show which one fits the player's headset.")
    @GetMapping("/releases")
    public ResponseEntity<List<QuestReleaseResponse>> listReleases() {
        return ResponseEntity.ok(questModService.listReleases());
    }

    @Operation(summary = "Generate a personalized Quest mod", description = "Builds a copy of the chosen release with the "
            + "signed in player's session baked in, so installing it logs the headset in without any pairing dance. The "
            + "download contains a private credential; it must never be shared or cached. Omit the tag for the latest "
            + "release.")
    @PostMapping("/download")
    public ResponseEntity<byte[]> download(
            @RequestParam(required = false) String tag,
            @AuthenticationPrincipal PlayerUserDetails principal) {
        if (principal == null) {
            throw new UnauthorizedException("Player authentication required");
        }
        GeneratedMod mod = questModService.generate(principal.getUserId(), tag);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + mod.fileName() + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(mod.bytes());
    }
}
