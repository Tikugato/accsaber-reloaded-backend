package com.accsaber.backend.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.accsaber.backend.model.entity.staff.StaffUserStatus;
import com.accsaber.backend.repository.staff.StaffUserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Order(-100)
@ConditionalOnProperty(name = "accsaber.staging-gate.enabled", havingValue = "true")
@RequiredArgsConstructor
public class StagingGateFilter extends OncePerRequestFilter {

    private static final Duration CACHE_TTL = Duration.ofMinutes(15);
    private static final String REALM = "AccSaber staging - sign in with your staff account";
    public static final String KEY_HEADER = "X-Staging-Key";

    private final StaffUserRepository staffUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${accsaber.staging-gate.key:}")
    private String gateKey;

    private final Map<String, Instant> accepted = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/health")
                || path.equals("/v1/health/ping")
                || path.equals("/robots.txt")
                || path.startsWith("/ws/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (hasValidKey(request)) {
            chain.doFilter(request, response);
            return;
        }
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Basic ") && isStaff(header.substring(6))) {
            chain.doFilter(request, response);
            return;
        }
        response.setHeader("WWW-Authenticate", "Basic realm=\"" + REALM + "\", charset=\"UTF-8\"");
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Staging is restricted to AccSaber staff.");
    }

    private boolean hasValidKey(HttpServletRequest request) {
        if (gateKey == null || gateKey.isBlank()) {
            return false;
        }
        String presented = request.getHeader(KEY_HEADER);
        if (presented == null) {
            return false;
        }
        return MessageDigest.isEqual(
                presented.getBytes(StandardCharsets.UTF_8),
                gateKey.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isStaff(String encoded) {
        Instant seen = accepted.get(encoded);
        if (seen != null && seen.isAfter(Instant.now())) {
            return true;
        }
        accepted.remove(encoded);

        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return false;
        }
        int separator = decoded.indexOf(':');
        if (separator < 1) {
            return false;
        }
        String username = decoded.substring(0, separator);
        String password = decoded.substring(separator + 1);

        boolean valid = staffUserRepository.findByUsernameIgnoreCaseAndActiveTrue(username).stream()
                .filter(staff -> staff.getStatus() == StaffUserStatus.ACCEPTED)
                .filter(staff -> staff.getPassword() != null)
                .anyMatch(staff -> passwordEncoder.matches(password, staff.getPassword()));

        if (valid) {
            accepted.put(encoded, Instant.now().plus(CACHE_TTL));
            log.info("Staging gate opened for staff '{}'", username);
        } else {
            log.warn("Staging gate rejected a sign in for '{}'", username);
        }
        return valid;
    }
}
