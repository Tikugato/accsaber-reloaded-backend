package com.accsaber.backend.config;

import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PublicApiCorsConfigurationSource implements CorsConfigurationSource {

    private static final String VERSION_PREFIX = "/v1/";

    private final CorsConfiguration firstParty;
    private final CorsConfiguration publicRead;
    private final Set<String> publicReadPrefixes;

    @Override
    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null) {
            return null;
        }
        if (firstParty.checkOrigin(origin) != null) {
            return firstParty;
        }
        return isPublicRead(request.getRequestURI()) ? publicRead : firstParty;
    }

    private boolean isPublicRead(String path) {
        return publicReadPrefixes.contains(resolvePrefix(path));
    }

    private static String resolvePrefix(String path) {
        int start = path.startsWith(VERSION_PREFIX) ? VERSION_PREFIX.length() : 1;
        int separator = path.indexOf('/', start);
        return separator < 0 ? path : path.substring(0, separator);
    }
}
