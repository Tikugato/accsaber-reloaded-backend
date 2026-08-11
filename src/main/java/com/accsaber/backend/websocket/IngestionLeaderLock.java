package com.accsaber.backend.websocket;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class IngestionLeaderLock {

    private static final int LOCK_CLASS = 8244;
    private static final int LOCK_KEY = 1;

    private final String url;
    private final String username;
    private final String password;

    private Connection held;

    public IngestionLeaderLock(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public synchronized boolean acquire() {
        if (holdsValidConnection()) {
            return true;
        }
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(url, username, password);
            if (tryLock(connection)) {
                held = connection;
                log.info("Acquired score ingestion leadership");
                return true;
            }
            connection.close();
            return false;
        } catch (Exception e) {
            log.warn("Could not acquire score ingestion leadership: {}", e.getMessage());
            closeQuietly(connection);
            return false;
        }
    }

    @PreDestroy
    public synchronized void release() {
        if (held == null) {
            return;
        }
        try (PreparedStatement statement = held.prepareStatement("SELECT pg_advisory_unlock(?, ?)")) {
            statement.setInt(1, LOCK_CLASS);
            statement.setInt(2, LOCK_KEY);
            statement.execute();
            log.info("Released score ingestion leadership");
        } catch (Exception e) {
            log.warn("Could not release score ingestion leadership cleanly: {}", e.getMessage());
        }
        closeQuietly(held);
        held = null;
    }

    private boolean holdsValidConnection() {
        if (held == null) {
            return false;
        }
        try {
            if (held.isValid(2)) {
                return true;
            }
            log.warn("Score ingestion leadership connection went stale, giving it up");
        } catch (Exception e) {
            log.warn("Score ingestion leadership connection check failed: {}", e.getMessage());
        }
        closeQuietly(held);
        held = null;
        return false;
    }

    private boolean tryLock(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_try_advisory_lock(?, ?)")) {
            statement.setInt(1, LOCK_CLASS);
            statement.setInt(2, LOCK_KEY);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getBoolean(1);
            }
        }
    }

    private void closeQuietly(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (Exception e) {
            log.debug("Closing score ingestion leadership connection failed: {}", e.getMessage());
        }
    }
}
