package com.accsaber.backend.controller.user;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.accsaber.backend.exception.UnauthorizedException;
import com.accsaber.backend.model.dto.request.user.ProfileUpdateRequest;
import com.accsaber.backend.security.PlayerUserDetails;
import com.accsaber.backend.service.player.ProfileCustomizationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/users/me")
@RequiredArgsConstructor
@Tag(name = "Players")
public class ProfileCustomizationController {

    private final ProfileCustomizationService profileService;

    @Operation(summary = "Update your profile", description = "Change your name, bio or pinned scores. Send only the fields you "
            + "want to touch and the rest are left alone. Worth knowing that setting a name here turns off the sync that "
            + "normally pulls your name across from BeatLeader or ScoreSaber, since otherwise the next refresh would undo "
            + "your change. You can turn it back on through the sync.name setting. Bios get cleaned up server side, and "
            + "pinned scores are replaced as a set rather than added to.")
    @PatchMapping("/profile")
    public ResponseEntity<Void> updateProfile(
            @RequestBody ProfileUpdateRequest request,
            @AuthenticationPrincipal PlayerUserDetails principal) {
        Long userId = requirePrincipal(principal).getUserId();
        if (request.getName() != null) {
            profileService.updateName(userId, request.getName());
        }
        if (request.getBio() != null) {
            profileService.updateBio(userId, request.getBio());
        }
        if (request.getPinnedScores() != null) {
            profileService.updatePinnedScores(userId, request.getPinnedScores());
        }
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Upload your avatar", description = "Sets a custom avatar, replacing whatever was there before. Like "
            + "the name, this switches off the sync that pulls your avatar from the platforms so a later refresh does not "
            + "overwrite it. Turn it back on with the sync.avatar setting. You get the URL of the stored image back, and you "
            + "should use that rather than guessing the extension, since what we store depends on what you sent.")
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadAvatar(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal PlayerUserDetails principal) {
        Long userId = requirePrincipal(principal).getUserId();
        return ResponseEntity.ok(profileService.updateAvatar(userId, file));
    }

    private PlayerUserDetails requirePrincipal(PlayerUserDetails principal) {
        if (principal == null) {
            throw new UnauthorizedException("Player authentication required");
        }
        return principal;
    }
}
