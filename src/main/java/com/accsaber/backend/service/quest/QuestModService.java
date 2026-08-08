package com.accsaber.backend.service.quest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Service;

import com.accsaber.backend.client.GitHubReleaseClient;
import com.accsaber.backend.client.GitHubReleaseClient.GitHubAsset;
import com.accsaber.backend.client.GitHubReleaseClient.GitHubRelease;
import com.accsaber.backend.exception.ResourceNotFoundException;
import com.accsaber.backend.exception.TooManyRequestsException;
import com.accsaber.backend.exception.ValidationException;
import com.accsaber.backend.model.dto.response.PlayerAuthResponse;
import com.accsaber.backend.model.dto.response.quest.QuestReleaseResponse;
import com.accsaber.backend.service.oauth.OauthService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestModService {

    public record GeneratedMod(String fileName, byte[] bytes) {
    }

    private static final String EXPECTED_MOD_ID = "AccSaberQLite";
    private static final String EXPECTED_PACKAGE_ID = "com.beatgames.beatsaber";
    private static final String MOD_JSON_ENTRY = "mod.json";
    private static final String TOKEN_FILE_NAME = "accsaber_session_DO_NOT_SHARE.txt";
    private static final String TOKEN_DESTINATION =
            "/sdcard/ModData/com.beatgames.beatsaber/Mods/AccSaberQLite/" + TOKEN_FILE_NAME;
    private static final long MAX_ASSET_BYTES = 20L * 1024 * 1024;
    private static final int MAX_GENERATES_PER_HOUR = 5;

    private final GitHubReleaseClient gitHubReleaseClient;
    private final OauthService oauthService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Cache<String, List<GitHubRelease>> releaseCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(1)
            .build();
    private final Cache<String, byte[]> assetCache = Caffeine.newBuilder()
            .maximumSize(12)
            .build();
    private final Cache<Long, AtomicInteger> generateCounts = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .maximumSize(50_000)
            .build();

    public List<QuestReleaseResponse> listReleases() {
        List<GitHubRelease> releases = cachedReleases();
        boolean latestSeen = false;
        java.util.ArrayList<QuestReleaseResponse> result = new java.util.ArrayList<>();
        for (GitHubRelease release : releases) {
            GitHubAsset asset = qmodAsset(release);
            if (asset == null) {
                continue;
            }
            boolean latest = !latestSeen && !release.prerelease();
            if (latest) {
                latestSeen = true;
            }
            result.add(QuestReleaseResponse.builder()
                    .tag(release.tag())
                    .name(release.name())
                    .gameVersion(gameVersionOf(asset))
                    .publishedAt(release.publishedAt())
                    .prerelease(release.prerelease())
                    .latest(latest)
                    .build());
        }
        return result;
    }

    public GeneratedMod generate(Long userId, String tag) {
        AtomicInteger count = generateCounts.get(userId, id -> new AtomicInteger());
        if (count.incrementAndGet() > MAX_GENERATES_PER_HOUR) {
            throw new TooManyRequestsException("Too many downloads generated; try again in an hour");
        }

        GitHubRelease release = resolveRelease(tag);
        GitHubAsset asset = qmodAsset(release);
        if (asset == null) {
            throw new ResourceNotFoundException("Quest mod release", tag == null ? "latest" : tag);
        }
        byte[] original = fetchAsset(asset);

        PlayerAuthResponse session = oauthService.issueGameSessionForUser(userId);
        byte[] personalized = injectSessionFile(original, session.getRefreshToken());

        log.info("Generated personalized Quest mod {} for user {}", release.tag(), userId);
        return new GeneratedMod("AccSaber-Lite_" + release.tag() + ".qmod", personalized);
    }

    private List<GitHubRelease> cachedReleases() {
        return releaseCache.get("releases", key -> gitHubReleaseClient.listReleases());
    }

    private GitHubRelease resolveRelease(String tag) {
        List<GitHubRelease> releases = cachedReleases();
        if (tag == null || tag.isBlank()) {
            return releases.stream()
                    .filter(r -> !r.prerelease() && qmodAsset(r) != null)
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Quest mod release", "latest"));
        }
        return releases.stream()
                .filter(r -> r.tag().equals(tag))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Quest mod release", tag));
    }

    private static GitHubAsset qmodAsset(GitHubRelease release) {
        return release.assets().stream()
                .filter(a -> a.name().endsWith(".qmod"))
                .filter(a -> a.size() > 0 && a.size() <= MAX_ASSET_BYTES)
                .findFirst()
                .orElse(null);
    }

    private byte[] fetchAsset(GitHubAsset asset) {
        byte[] bytes = assetCache.get(asset.downloadUrl(),
                url -> gitHubReleaseClient.downloadAsset(url).orElse(null));
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_ASSET_BYTES) {
            assetCache.invalidate(asset.downloadUrl());
            throw new ValidationException("Quest mod asset could not be fetched");
        }
        validateModJson(bytes);
        return bytes;
    }

    private String gameVersionOf(GitHubAsset asset) {
        try {
            byte[] bytes = assetCache.get(asset.downloadUrl(),
                    url -> gitHubReleaseClient.downloadAsset(url).orElse(null));
            if (bytes == null) {
                return null;
            }
            JsonNode modJson = readModJson(bytes);
            return modJson == null ? null : modJson.path("packageVersion").asText(null);
        } catch (Exception e) {
            return null;
        }
    }

    private void validateModJson(byte[] qmod) {
        JsonNode modJson = readModJson(qmod);
        if (modJson == null) {
            throw new ValidationException("Quest mod asset has no mod.json");
        }
        if (!EXPECTED_MOD_ID.equals(modJson.path("id").asText())) {
            throw new ValidationException("Quest mod asset has an unexpected mod id");
        }
        if (!EXPECTED_PACKAGE_ID.equals(modJson.path("packageId").asText())) {
            throw new ValidationException("Quest mod asset targets an unexpected package");
        }
    }

    private JsonNode readModJson(byte[] qmod) {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(qmod))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (MOD_JSON_ENTRY.equals(entry.getName())) {
                    return objectMapper.readTree(zip.readAllBytes());
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] injectSessionFile(byte[] qmod, String refreshToken) {
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(qmod));
                ByteArrayOutputStream buffer = new ByteArrayOutputStream(qmod.length + 4096)) {
            try (ZipOutputStream out = new ZipOutputStream(buffer)) {
                ZipEntry entry;
                while ((entry = in.getNextEntry()) != null) {
                    if (TOKEN_FILE_NAME.equals(entry.getName())) {
                        continue;
                    }
                    byte[] content = in.readAllBytes();
                    if (MOD_JSON_ENTRY.equals(entry.getName())) {
                        content = withTokenFileCopy(content);
                    }
                    out.putNextEntry(new ZipEntry(entry.getName()));
                    out.write(content);
                    out.closeEntry();
                }
                out.putNextEntry(new ZipEntry(TOKEN_FILE_NAME));
                out.write(refreshToken.getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
            return buffer.toByteArray();
        } catch (Exception e) {
            throw new ValidationException("Failed to personalize Quest mod: " + e.getMessage());
        }
    }

    private byte[] withTokenFileCopy(byte[] modJsonBytes) throws Exception {
        ObjectNode modJson = (ObjectNode) objectMapper.readTree(modJsonBytes);
        ArrayNode fileCopies = modJson.withArray("fileCopies");
        for (JsonNode copy : fileCopies) {
            if (TOKEN_FILE_NAME.equals(copy.path("name").asText())) {
                return objectMapper.writeValueAsBytes(modJson);
            }
        }
        ObjectNode copy = fileCopies.addObject();
        copy.put("name", TOKEN_FILE_NAME);
        copy.put("destination", TOKEN_DESTINATION);
        return objectMapper.writeValueAsBytes(modJson);
    }
}
