package com.accsaber.backend.service.oauth;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Service
public class PairingCodeService {

    public static final long TTL_SECONDS = 600;

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;

    private final SecureRandom random = new SecureRandom();
    private final Cache<String, Long> codes = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(TTL_SECONDS))
            .maximumSize(10_000)
            .build();

    public String create(Long userId) {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        String generated = code.toString();
        codes.put(generated, userId);
        return generated;
    }

    public Long consume(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return codes.asMap().remove(code.trim().toUpperCase(Locale.ROOT));
    }
}
