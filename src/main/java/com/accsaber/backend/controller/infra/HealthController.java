package com.accsaber.backend.controller.infra;

import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/v1/health")
@Tag(name = "Platform")
public class HealthController {

    private final ObjectProvider<BuildProperties> buildProperties;
    private final String releaseChannel;

    public HealthController(ObjectProvider<BuildProperties> buildProperties,
            @Value("${accsaber.release-channel:}") String releaseChannel) {
        this.buildProperties = buildProperties;
        this.releaseChannel = releaseChannel;
    }

    @Operation(summary = "Check the API is up", description = "Comes back with the service status, the current server time, the version "
            + "that is running and which release channel it belongs to. Handy as a quick connectivity check before "
            + "you start firing real requests at anything, and the version and channel are there so you can show "
            + "people which build they are talking to. The channel is empty once a version is a full release.")
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        log.info("Ping received");
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "timestamp", Instant.now(),
                "service", "accsaber-backend",
                "version", version(),
                "channel", releaseChannel));
    }

    private String version() {
        BuildProperties build = buildProperties.getIfAvailable();
        return build != null ? build.getVersion() : "dev";
    }
}
