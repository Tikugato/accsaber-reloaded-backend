package com.accsaber.backend.controller.admin;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.accsaber.backend.model.dto.response.PlayerAuthResponse;
import com.accsaber.backend.service.staff.ImpersonationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/admin/impersonate")
@ConditionalOnProperty(name = "accsaber.impersonation.enabled", havingValue = "true")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin - Players")
public class AdminImpersonationController {

    private final ImpersonationService impersonationService;

    @Operation(summary = "Get a player token for any account so you can browse the site exactly as they see it", description = "Only exists on non production environments, where it is enabled deliberately. "
            + "You get back the same shape as a normal player login, so you can drop the access token straight into "
            + "the frontend and everything behaves as if that player signed in. There is no refresh token, the token "
            + "is short lived, and every use is logged with your staff id against the account you acted as.")
    @PostMapping("/{userId}")
    public ResponseEntity<PlayerAuthResponse> impersonate(@PathVariable Long userId, Authentication authentication) {
        return ResponseEntity.ok(impersonationService.impersonate(userId, authentication.getName()));
    }
}
