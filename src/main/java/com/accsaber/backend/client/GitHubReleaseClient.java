package com.accsaber.backend.client;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GitHubReleaseClient {

    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(30);

    public record GitHubAsset(String name, long size, String downloadUrl) {
    }

    public record GitHubRelease(String tag, String name, Instant publishedAt, boolean prerelease,
            List<GitHubAsset> assets) {
    }

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${accsaber.quest.github-repo:accsaber/accsaber-qlite-plugin}")
    private String repo;

    public GitHubReleaseClient(@Qualifier("gitHubWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public List<GitHubRelease> listReleases() {
        try {
            String body = webClient.get()
                    .uri("/repos/{repo}/releases?per_page=20", repo)
                    .header("Accept", "application/vnd.github+json")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(HTTP_TIMEOUT);
            if (body == null) {
                return List.of();
            }

            JsonNode root = objectMapper.readTree(body);
            if (!root.isArray()) {
                return List.of();
            }
            List<GitHubRelease> releases = new ArrayList<>();
            for (JsonNode release : root) {
                if (release.path("draft").asBoolean(false)) {
                    continue;
                }
                List<GitHubAsset> assets = new ArrayList<>();
                for (JsonNode asset : release.path("assets")) {
                    assets.add(new GitHubAsset(
                            asset.path("name").asText(""),
                            asset.path("size").asLong(0),
                            asset.path("browser_download_url").asText("")));
                }
                releases.add(new GitHubRelease(
                        release.path("tag_name").asText(""),
                        release.path("name").asText(""),
                        parseInstant(release.path("published_at").asText(null)),
                        release.path("prerelease").asBoolean(false),
                        assets));
            }
            return releases;
        } catch (Exception e) {
            log.error("Failed to list GitHub releases for {}: {}", repo, e.getMessage());
            return List.of();
        }
    }

    public Optional<byte[]> downloadAsset(String downloadUrl) {
        try {
            return Optional.ofNullable(webClient.get()
                    .uri(java.net.URI.create(downloadUrl))
                    .header("Accept", "application/octet-stream")
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block(HTTP_TIMEOUT));
        } catch (Exception e) {
            log.error("Failed to download GitHub asset {}: {}", downloadUrl, e.getMessage());
            return Optional.empty();
        }
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception e) {
            return null;
        }
    }
}
