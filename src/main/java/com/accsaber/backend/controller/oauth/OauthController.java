package com.accsaber.backend.controller.oauth;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import com.accsaber.backend.config.OauthProperties;
import com.accsaber.backend.exception.AccSaberException;
import com.accsaber.backend.exception.UnauthorizedException;
import com.accsaber.backend.exception.ValidationException;
import com.accsaber.backend.model.dto.request.RefreshTokenRequest;
import com.accsaber.backend.model.dto.request.auth.IngameAuthRequest;
import com.accsaber.backend.model.dto.response.AuthMeResponse;
import com.accsaber.backend.model.dto.response.PlayerAuthResponse;
import com.accsaber.backend.security.PlayerUserDetails;
import com.accsaber.backend.service.oauth.OauthService;
import com.accsaber.backend.service.oauth.OauthService.DiscordCallbackOutcome;
import com.accsaber.backend.service.oauth.OauthStateService;
import com.accsaber.backend.service.oauth.OauthStateService.StateClaims;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth")
public class OauthController {

    private static final Set<String> KNOWN_PROVIDERS = Set.of(
            OauthService.PROVIDER_DISCORD,
            OauthService.PROVIDER_BEATLEADER,
            OauthService.PROVIDER_STEAM);

    private static final Set<String> INGAME_TICKET_PROVIDERS = Set.of("steamTicket", "oculusTicket");

    private final OauthService oauthService;
    private final OauthStateService stateService;
    private final OauthProperties oauthProperties;

    @Operation(summary = "Start signing in", description = "Kicks off the sign in dance with one of discord, beatleader or "
            + "steam. Send the browser here rather than calling it with fetch, because it answers with a redirect off to the "
            + "provider. returnTo is where we send the player back afterwards and has to be one of our own domains. If you are "
            + "already signed in when you hit this, the new provider gets attached to your existing account instead of making "
            + "a second one.")
    @GetMapping("/{provider}/start")
    public ResponseEntity<Void> start(
            @PathVariable String provider,
            @RequestParam String returnTo,
            @RequestParam(required = false) String pendingLinkToken,
            @AuthenticationPrincipal PlayerUserDetails principal) {

        requireKnownProvider(provider);
        validateReturnTo(returnTo);
        Long linkUserId = principal != null ? principal.getUserId() : null;

        String state = stateService.createState(provider, returnTo, linkUserId, pendingLinkToken);
        String authorizeUrl = OauthService.PROVIDER_STEAM.equals(provider)
                ? oauthService.buildStartUrl(provider, state,
                        appendQuery(oauthProperties.getSteam().getReturnTo(), "state", state))
                : oauthService.buildStartUrl(provider, state, null);

        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(authorizeUrl)).build();
    }

    @Operation(summary = "Discord sign in callback", description = "Where Discord sends the player back to. You do not call "
            + "this yourself, the browser lands on it and gets redirected on to whatever returnTo you set at the start.")
    @GetMapping("/discord/callback")
    public ResponseEntity<Void> discordCallback(@RequestParam String code, @RequestParam String state) {
        StateClaims claims = stateService.parseState(state, OauthService.PROVIDER_DISCORD);
        try {
            DiscordCallbackOutcome outcome = oauthService.handleDiscordCallback(
                    code, claims.linkUserId(), claims.pendingLinkToken(), claims.returnTo());
            return switch (outcome) {
                case DiscordCallbackOutcome.Session s -> redirectWithSession(claims.returnTo(), s.response());
                case DiscordCallbackOutcome.PendingLink p -> redirectWithFragment(
                        claims.returnTo(), "pendingLinkToken=" + encode(p.pendingLinkToken()));
            };
        } catch (AccSaberException e) {
            return redirectWithError(claims.returnTo(), e);
        }
    }

    @Operation(summary = "BeatLeader sign in callback", description = "The same idea as the Discord one, for players coming "
            + "back from BeatLeader.")
    @GetMapping("/beatleader/callback")
    public ResponseEntity<Void> beatLeaderCallback(@RequestParam String code, @RequestParam String state) {
        StateClaims claims = stateService.parseState(state, OauthService.PROVIDER_BEATLEADER);
        try {
            PlayerAuthResponse response = oauthService.handleBeatLeaderCallback(
                    code, claims.linkUserId(), claims.pendingLinkToken());
            return redirectWithSession(claims.returnTo(), response);
        } catch (AccSaberException e) {
            return redirectWithError(claims.returnTo(), e);
        }
    }

    @Operation(summary = "Steam sign in callback", description = "Where Steam sends the player back to. Steam uses OpenID "
            + "rather than OAuth so the parameters on the way in look different, but from your side it behaves the same as the "
            + "other two.")
    @GetMapping("/steam/callback")
    public ResponseEntity<Void> steamCallback(@RequestParam("state") String state, HttpServletRequest request) {
        StateClaims claims = stateService.parseState(state, OauthService.PROVIDER_STEAM);
        try {
            PlayerAuthResponse response = oauthService.handleSteamCallback(
                    openidParams(request), claims.linkUserId(), claims.pendingLinkToken());
            return redirectWithSession(claims.returnTo(), response);
        } catch (AccSaberException e) {
            return redirectWithError(claims.returnTo(), e);
        }
    }

    @Operation(summary = "Sign in from inside the game", description = "How the game plugin authenticates, since it has no "
            + "browser to run a redirect flow in. It hands over a platform ticket from Steam or Oculus and gets a player token "
            + "back. Tokens minted here are the only ones allowed to submit scores, so a token from the website will be turned "
            + "away by the submit route. The answer also carries roles, every staff role the player holds, so a build of the "
            + "plugin can show staff only options. It is a hint for the interface and nothing more, the roles it lists do not "
            + "grant the token any extra power on its own.")
    @PostMapping("/ingame")
    public ResponseEntity<PlayerAuthResponse> ingame(@Valid @RequestBody IngameAuthRequest request) {
        if (!INGAME_TICKET_PROVIDERS.contains(request.getProvider())) {
            throw new ValidationException("Unknown ticket provider: " + request.getProvider());
        }
        return ResponseEntity.ok(oauthService.handleIngameTicket(request.getProvider(), request.getTicket()));
    }

    @Operation(summary = "Refresh your session", description = "Trade a refresh token for a fresh access token so the player "
            + "does not have to sign in again. Worth doing before the old one expires rather than waiting for a 401.")
    @PostMapping("/refresh")
    public ResponseEntity<PlayerAuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(oauthService.refresh(request.getRefreshToken()));
    }

    @Operation(summary = "Log out", description = "Ends the current session and invalidates its refresh token.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        oauthService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Find out who you are", description = "Tells you which player the token belongs to, along with the "
            + "providers connected to the account and any staff roles they carry. Handy right after a sign in to work out who "
            + "just came back.")
    @GetMapping("/me")
    public ResponseEntity<AuthMeResponse> me(@AuthenticationPrincipal PlayerUserDetails principal) {
        PlayerUserDetails player = requirePrincipal(principal);
        return ResponseEntity.ok(oauthService.getMe(player.getUserId(), player.getStaffId()));
    }

    @Operation(summary = "Disconnect a provider", description = "Detaches one of discord, beatleader or steam from your "
            + "account. You cannot remove the last one, since that would leave nothing to sign in with.")
    @DeleteMapping("/connections/{provider}")
    public ResponseEntity<Void> removeConnection(
            @PathVariable String provider,
            @AuthenticationPrincipal PlayerUserDetails principal) {
        requireKnownProvider(provider);
        oauthService.removeConnection(requirePrincipal(principal).getUserId(), provider);
        return ResponseEntity.noContent().build();
    }

    private void requireKnownProvider(String provider) {
        if (!KNOWN_PROVIDERS.contains(provider)) {
            throw new ValidationException("Unknown OAuth provider: " + provider);
        }
    }

    private PlayerUserDetails requirePrincipal(PlayerUserDetails principal) {
        if (principal == null) {
            throw new UnauthorizedException("Player authentication required");
        }
        return principal;
    }

    private Map<String, String> openidParams(HttpServletRequest request) {
        Map<String, String> out = new HashMap<>();
        request.getParameterMap().forEach((k, v) -> {
            if (k.startsWith("openid.") && v.length > 0) {
                out.put(k, v[0]);
            }
        });
        return out;
    }

    private ResponseEntity<Void> redirectWithSession(String returnTo, PlayerAuthResponse response) {
        return redirectWithFragment(returnTo,
                "accessToken=" + encode(response.getAccessToken())
                        + "&refreshToken=" + encode(response.getRefreshToken())
                        + "&expiresIn=" + response.getExpiresIn()
                        + "&userId=" + response.getUserId());
    }

    private ResponseEntity<Void> redirectWithFragment(String returnTo, String fragment) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(returnTo + "#" + fragment));
        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
    }

    private ResponseEntity<Void> redirectWithError(String returnTo, AccSaberException e) {
        log.info("OAuth callback failed: {}", e.getMessage());
        return redirectWithFragment(returnTo,
                "error=" + encode(e.getErrorCode().toLowerCase())
                        + "&message=" + encode(e.getMessage()));
    }

    private void validateReturnTo(String returnTo) {
        var allowed = oauthProperties.getAllowedReturnOrigins();
        if (allowed == null || allowed.isEmpty()) {
            throw new ValidationException("OAuth allowed-return-origins is not configured");
        }
        String inboundOrigin = originOf(parseUri(returnTo));
        if (inboundOrigin == null) {
            throw new ValidationException("returnTo must be an absolute URL");
        }
        boolean ok = allowed.stream()
                .map(origin -> originOf(parseUri(origin)))
                .anyMatch(inboundOrigin::equalsIgnoreCase);
        if (!ok) {
            throw new ValidationException("returnTo is not in the allowed origin list");
        }
    }

    private URI parseUri(String value) {
        try {
            return URI.create(value);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Invalid URL: " + value);
        }
    }

    private String originOf(URI uri) {
        if (uri.getScheme() == null || uri.getHost() == null) {
            return null;
        }
        int port = uri.getPort();
        return port < 0
                ? uri.getScheme() + "://" + uri.getHost()
                : uri.getScheme() + "://" + uri.getHost() + ":" + port;
    }

    private String appendQuery(String url, String key, String value) {
        return url + (url.contains("?") ? "&" : "?") + key + "=" + encode(value);
    }

    private static String encode(String s) {
        return UriUtils.encode(s, StandardCharsets.UTF_8);
    }
}
