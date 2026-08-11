package com.accsaber.backend.websocket;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import com.accsaber.backend.config.PlatformProperties;
import com.accsaber.backend.service.score.ScoreIngestionService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class WebSocketConnectionManager implements SmartLifecycle {

    private final PlatformProperties properties;
    private final ScoreIngestionService scoreIngestionService;
    private final IngestionLeaderLock leaderLock;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService reconnectScheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger blReconnectMs = new AtomicInteger();
    private final AtomicInteger ssReconnectMs = new AtomicInteger();

    private volatile BeatLeaderWebSocketListener blListener;
    private volatile ScoreSaberWebSocketListener ssListener;

    public WebSocketConnectionManager(PlatformProperties properties,
            ScoreIngestionService scoreIngestionService,
            IngestionLeaderLock leaderLock) {
        this.properties = properties;
        this.scoreIngestionService = scoreIngestionService;
        this.leaderLock = leaderLock;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(
                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.blReconnectMs.set(properties.getWsReconnectIntervalMs());
        this.ssReconnectMs.set(properties.getWsReconnectIntervalMs());
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            long interval = properties.getWsReconnectIntervalMs();
            long startDelay = Math.max(0, properties.getWsStartDelaySeconds()) * 1000L;
            reconnectScheduler.schedule(this::connectAll, startDelay, TimeUnit.MILLISECONDS);
            reconnectScheduler.scheduleWithFixedDelay(this::monitorConnections, startDelay + interval, interval,
                    TimeUnit.MILLISECONDS);
            log.info("WebSocket connection manager started, connecting in {}ms", startDelay);
        }
    }

    private void connectAll() {
        if (!running.get()) {
            return;
        }
        if (!leaderLock.acquire()) {
            log.info("Another instance owns score ingestion - standing by until it releases");
            return;
        }
        connectBeatLeader();
        connectScoreSaber();
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            closeListener(blListener);
            closeListener(ssListener);
            reconnectScheduler.shutdown();
            leaderLock.release();
            log.info("WebSocket connection manager stopped");
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    public Map<String, Object> getStatus() {
        return Map.of(
                "beatleader", Map.of(
                        "connected", blListener != null && blListener.isOpen(),
                        "lastDisconnectedAt", blListener != null && blListener.getLastDisconnectedAt() != null
                                ? blListener.getLastDisconnectedAt().toString()
                                : "never"),
                "scoresaber", Map.of(
                        "connected", ssListener != null && ssListener.isOpen(),
                        "lastDisconnectedAt", ssListener != null && ssListener.getLastDisconnectedAt() != null
                                ? ssListener.getLastDisconnectedAt().toString()
                                : "never",
                        "lastMessageReceivedAt", ssListener != null && ssListener.getLastMessageReceivedAt() != null
                                ? ssListener.getLastMessageReceivedAt().toString()
                                : "never"));
    }

    public void reconnect(String platform) {
        if ("beatleader".equalsIgnoreCase(platform)) {
            closeListener(blListener);
            blReconnectMs.set(properties.getWsReconnectIntervalMs());
            connectBeatLeader();
        } else if ("scoresaber".equalsIgnoreCase(platform)) {
            closeListener(ssListener);
            ssReconnectMs.set(properties.getWsReconnectIntervalMs());
            connectScoreSaber();
        }
    }

    public boolean isBeatLeaderConnected() {
        return blListener != null && blListener.isOpen();
    }

    public boolean isScoreSaberConnected() {
        return ssListener != null && ssListener.isOpen();
    }

    private void connectBeatLeader() {
        String url = properties.getBeatleader().getWebsocketUrl();
        if (url == null || url.isBlank()) {
            log.warn("BeatLeader WebSocket URL not configured - skipping");
            return;
        }
        try {
            log.info("Connecting BeatLeader WebSocket to {}", url);
            blListener = new BeatLeaderWebSocketListener(new URI(url), scoreIngestionService, objectMapper);
            blListener.connect();
        } catch (Exception e) {
            log.error("Failed to connect BeatLeader WebSocket to {}: {}", url, e.getMessage());
        }
    }

    private void connectScoreSaber() {
        String url = properties.getScoresaber().getWebsocketUrl();
        if (url == null || url.isBlank()) {
            log.warn("ScoreSaber WebSocket URL not configured - skipping");
            return;
        }
        try {
            ssListener = new ScoreSaberWebSocketListener(new URI(url), scoreIngestionService, objectMapper);
            ssListener.connect();
        } catch (Exception e) {
            log.error("Failed to connect ScoreSaber WebSocket: {}", e.getMessage());
        }
    }

    private void monitorConnections() {
        if (!running.get())
            return;
        if (!leaderLock.acquire()) {
            if (blListener != null || ssListener != null) {
                log.warn("Lost score ingestion leadership - closing both sockets");
                closeListener(blListener);
                closeListener(ssListener);
                blListener = null;
                ssListener = null;
            }
            return;
        }
        int maxMs = properties.getWsMaxReconnectIntervalMs();
        int baseMs = properties.getWsReconnectIntervalMs();

        if (!isBeatLeaderConnected() && !isIntentionallyClosed(blListener)) {
            int currentMs = blReconnectMs.get();
            if (currentMs <= 0) {
                log.info("BeatLeader WebSocket disconnected - reconnecting");
                connectBeatLeader();
                blReconnectMs.set(Math.min(baseMs * 2, maxMs));
            } else {
                blReconnectMs.addAndGet(-baseMs);
            }
        } else if (isBeatLeaderConnected()) {
            blReconnectMs.set(baseMs);
        }

        boolean ssStale = isScoreSaberConnected() && isScoreSaberStale();
        if (ssStale) {
            log.warn("ScoreSaber WebSocket connected but stale (no messages received recently) - forcing reconnect");
            closeListener(ssListener);
            ssListener.setIntentionalClose(false);
        }

        if ((!isScoreSaberConnected() || ssStale) && !isIntentionallyClosed(ssListener)) {
            int currentMs = ssReconnectMs.get();
            if (currentMs <= 0) {
                log.info("ScoreSaber WebSocket disconnected - reconnecting");
                connectScoreSaber();
                ssReconnectMs.set(Math.min(baseMs * 2, maxMs));
            } else {
                ssReconnectMs.addAndGet(-baseMs);
            }
        } else if (isScoreSaberConnected()) {
            ssReconnectMs.set(baseMs);
        }
    }

    private boolean isScoreSaberStale() {
        if (ssListener == null) return false;
        Instant lastMsg = ssListener.getLastMessageReceivedAt();
        if (lastMsg == null) return false;
        long silenceMs = Instant.now().toEpochMilli() - lastMsg.toEpochMilli();
        return silenceMs > properties.getSsStaleTimeoutMs();
    }

    private boolean isIntentionallyClosed(Object listener) {
        if (listener instanceof BeatLeaderWebSocketListener bl)
            return bl.isIntentionalClose();
        if (listener instanceof ScoreSaberWebSocketListener ss)
            return ss.isIntentionalClose();
        return false;
    }

    private void closeListener(Object listener) {
        if (listener instanceof BeatLeaderWebSocketListener bl) {
            bl.setIntentionalClose(true);
            bl.close();
        } else if (listener instanceof ScoreSaberWebSocketListener ss) {
            ss.setIntentionalClose(true);
            ss.close();
        }
    }
}
