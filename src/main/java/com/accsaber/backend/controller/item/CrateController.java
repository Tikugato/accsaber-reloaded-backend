package com.accsaber.backend.controller.item;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.accsaber.backend.exception.UnauthorizedException;
import com.accsaber.backend.model.dto.response.item.CrateContentResponse;
import com.accsaber.backend.model.dto.response.item.CrateModifierResponse;
import com.accsaber.backend.model.dto.response.item.CrateOpenResponse;
import com.accsaber.backend.model.dto.response.item.UnusualEffectResponse;
import com.accsaber.backend.security.PlayerUserDetails;
import com.accsaber.backend.service.item.CrateService;
import com.accsaber.backend.service.item.ItemMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "Items and Market")
public class CrateController {

    private final CrateService crateService;

    @Operation(summary = "See what is inside a crate", description = "What a crate can give you, with each chance normalised so "
            + "the pool adds up to one. Only the items marked visible are listed, so a crate holding something unreleased will "
            + "show a pool that does not quite account for everything.")
    @GetMapping("/crates/{crateItemId}/contents")
    public ResponseEntity<List<CrateContentResponse>> listContents(@PathVariable UUID crateItemId) {
        return ResponseEntity.ok(ItemMapper.toCrateContentResponses(crateService.listVisibleContents(crateItemId)));
    }

    @Operation(summary = "See the modifiers a crate can roll", description = "The modifiers this crate might put on whatever it "
            + "gives you, each with its own chance. Modifiers are rolled separately from the item itself, so the same item out "
            + "of the same crate can come out looking different.")
    @GetMapping("/crates/{crateItemId}/modifiers")
    public ResponseEntity<List<CrateModifierResponse>> listModifiers(@PathVariable UUID crateItemId) {
        return ResponseEntity.ok(ItemMapper.toCrateModifierResponses(crateService.listModifiers(crateItemId)));
    }

    @Operation(summary = "See the unusual effects a crate can roll", description = "Which unusual effects this crate can "
            + "produce. Unlike the item pool these are all equally likely, so there is no per effect chance to read here.")
    @GetMapping("/crates/{crateItemId}/unusual-effects")
    public ResponseEntity<List<UnusualEffectResponse>> listUnusualEffects(@PathVariable UUID crateItemId) {
        return ResponseEntity.ok(crateService.listUnusualEffects(crateItemId).stream()
                .map(ItemMapper::toUnusualEffectResponse)
                .toList());
    }

    @Operation(summary = "Open a crate", description = "Opens one of your crates and rolls you a reward, along with any "
            + "modifier or unusual effect that came with it. The crate is consumed either way. Openings are broadcast on the "
            + "crate feed, so if you are showing an animation the result is already on its way to everyone else too.")
    @PostMapping("/users/me/crates/{linkId}/open")
    public ResponseEntity<CrateOpenResponse> open(
            @PathVariable UUID linkId,
            @AuthenticationPrincipal PlayerUserDetails principal) {
        Long me = requirePrincipal(principal).getUserId();
        return ResponseEntity.ok(ItemMapper.toCrateOpenResponse(crateService.openCrate(me, linkId)));
    }

    private PlayerUserDetails requirePrincipal(PlayerUserDetails principal) {
        if (principal == null) {
            throw new UnauthorizedException("Player authentication required");
        }
        return principal;
    }
}
