package com.accsaber.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class PublicApiCorsConfigurationSourceTest {

    private static final String FIRST_PARTY_ORIGIN = "https://accsaber.com";
    private static final String THIRD_PARTY_ORIGIN = "https://overlay.example.com";

    private final CorsConfigurationSource source = new SecurityConfig()
            .corsConfigurationSource(List.of("accsaber.com"));

    @Test
    void returnsNoConfigurationWhenRequestCarriesNoOrigin() {
        assertThat(resolve(null, "/v1/curves")).isNull();
    }

    @Test
    void firstPartyOriginKeepsCredentialedConfigurationOnPublicPaths() {
        CorsConfiguration config = resolve(FIRST_PARTY_ORIGIN, "/v1/users/76561198000000000");

        assertThat(config).isNotNull();
        assertThat(config.getAllowCredentials()).isTrue();
        assertThat(config.checkOrigin(FIRST_PARTY_ORIGIN)).isEqualTo(FIRST_PARTY_ORIGIN);
    }

    @Test
    void firstPartyOriginKeepsCredentialedConfigurationOnPrivatePaths() {
        CorsConfiguration config = resolve(FIRST_PARTY_ORIGIN, "/v1/admin/campaigns");

        assertThat(config).isNotNull();
        assertThat(config.getAllowCredentials()).isTrue();
    }

    @Test
    void thirdPartyOriginGetsOpenReadOnlyConfigurationOnPublicPaths() {
        CorsConfiguration config = resolve(THIRD_PARTY_ORIGIN, "/v1/curves/points");

        assertThat(config).isNotNull();
        assertThat(config.getAllowCredentials()).isFalse();
        assertThat(config.checkOrigin(THIRD_PARTY_ORIGIN)).isEqualTo("*");
        assertThat(config.getAllowedMethods()).containsExactly("GET", "HEAD", "OPTIONS");
    }

    @Test
    void thirdPartyOriginGetsOpenConfigurationOnMediaPaths() {
        assertThat(resolve(THIRD_PARTY_ORIGIN, "/cdn/avatars/1.png").getAllowCredentials()).isFalse();
    }

    @Test
    void thirdPartyOriginIsRejectedOnPrivatePaths() {
        CorsConfiguration config = resolve(THIRD_PARTY_ORIGIN, "/v1/admin/campaigns");

        assertThat(config).isNotNull();
        assertThat(config.checkOrigin(THIRD_PARTY_ORIGIN)).isNull();
    }

    @Test
    void thirdPartyOriginIsRejectedOnAuthenticationPaths() {
        CorsConfiguration config = resolve(THIRD_PARTY_ORIGIN, "/v1/auth/discord/callback");

        assertThat(config.checkOrigin(THIRD_PARTY_ORIGIN)).isNull();
    }

    @Test
    void publicPrefixesMatchWholeSegmentsOnly() {
        CorsConfiguration config = resolve(THIRD_PARTY_ORIGIN, "/v1/curvesomething");

        assertThat(config.checkOrigin(THIRD_PARTY_ORIGIN)).isNull();
    }

    private CorsConfiguration resolve(String origin, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        if (origin != null) {
            request.addHeader("Origin", origin);
        }
        return source.getCorsConfiguration(request);
    }
}
