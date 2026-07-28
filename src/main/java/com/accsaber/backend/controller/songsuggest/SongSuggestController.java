package com.accsaber.backend.controller.songsuggest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.accsaber.backend.exception.ResourceNotFoundException;
import com.accsaber.backend.model.dto.response.songsuggest.SongSuggestRefreshTimeResponse;
import com.accsaber.backend.service.songsuggest.SongSuggestService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/songsuggest")
@RequiredArgsConstructor
@Tag(name = "Maps")
public class SongSuggestController {

        private final SongSuggestService songSuggestService;

        @Operation(summary = "Download the Song Suggest data", description = "A JSON file holding each qualifying player's top "
                        + "30 scores by raw AP, which is what Song Suggest uses to work out recommendations. It is rebuilt "
                        + "weekly rather than on demand, so check the refresh time first and only pull it again when it has "
                        + "actually changed. This comes back as a file download rather than an inline body.")
        @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Resource> getLeaderboard() {
                Path file = songSuggestService.getOutputFile();
                if (!Files.exists(file)) {
                        throw new ResourceNotFoundException("SongSuggestLeaderboard", "not yet generated");
                }
                Instant mtime = songSuggestService.getRefreshTime()
                                .orElseThrow(() -> new ResourceNotFoundException("SongSuggestLeaderboard",
                                                "not yet generated"));
                return ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.LAST_MODIFIED, mtime.toString())
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=\"AccSaberLeaderboardData.json\"")
                                .body(new FileSystemResource(file));
        }

        @Operation(summary = "Check when the Song Suggest data was last built", description = "Just the timestamp of the last "
                        + "rebuild. Cheap to call, so use it to decide whether the file above is worth downloading again.")
        @GetMapping("/refresh-time")
        public ResponseEntity<SongSuggestRefreshTimeResponse> getRefreshTime() {
                Instant mtime = songSuggestService.getRefreshTime()
                                .orElseThrow(() -> new ResourceNotFoundException("SongSuggestLeaderboard",
                                                "not yet generated"));
                return ResponseEntity.ok(SongSuggestRefreshTimeResponse.builder()
                                .refreshTime(mtime)
                                .build());
        }
}
