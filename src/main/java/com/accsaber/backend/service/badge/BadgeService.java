package com.accsaber.backend.service.badge;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.accsaber.backend.exception.ResourceNotFoundException;
import com.accsaber.backend.model.dto.response.badge.BadgeResponse;
import com.accsaber.backend.model.dto.response.badge.UserBadgeResponse;
import com.accsaber.backend.model.entity.badge.Badge;
import com.accsaber.backend.model.entity.badge.UserBadgeLink;
import com.accsaber.backend.model.entity.staff.StaffUser;
import com.accsaber.backend.repository.badge.BadgeRepository;
import com.accsaber.backend.repository.badge.UserBadgeLinkRepository;
import com.accsaber.backend.repository.user.UserRepository;
import com.accsaber.backend.service.player.DuplicateUserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeLinkRepository userBadgeLinkRepository;
    private final UserRepository userRepository;
    private final DuplicateUserService duplicateUserService;

    public List<BadgeResponse> findAllActive() {
        return badgeRepository.findByActiveTrue().stream()
                .map(BadgeService::toBadgeResponse)
                .toList();
    }

    public BadgeResponse findById(UUID id) {
        Badge badge = badgeRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Badge", id));
        return toBadgeResponse(badge);
    }

    public List<UserBadgeResponse> findUserBadges(Long userId) {
        Long resolved = duplicateUserService.resolvePrimaryUserId(userId);
        return userBadgeLinkRepository.findByUser_Id(resolved).stream()
                .map(BadgeService::toUserBadgeResponse)
                .toList();
    }

    @Transactional
    public BadgeResponse create(String name, String description, String imageUrl) {
        Badge badge = Badge.builder()
                .name(name)
                .description(description)
                .imageUrl(imageUrl)
                .build();
        return toBadgeResponse(badgeRepository.save(badge));
    }

    @Transactional
    public void deactivate(UUID id) {
        Badge badge = badgeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Badge", id));
        badge.setActive(false);
        badgeRepository.save(badge);
    }

    @Transactional
    public UserBadgeResponse awardBadge(Long userId, UUID badgeId, StaffUser awardedBy, String reason) {
        Long resolved = duplicateUserService.resolvePrimaryUserId(userId);
        if (!userRepository.existsById(resolved)) {
            throw new ResourceNotFoundException("User", resolved);
        }
        Badge badge = badgeRepository.findByIdAndActiveTrue(badgeId)
                .orElseThrow(() -> new ResourceNotFoundException("Badge", badgeId));

        if (userBadgeLinkRepository.existsByUser_IdAndBadge_Id(resolved, badgeId)) {
            return null;
        }

        UserBadgeLink link = UserBadgeLink.builder()
                .user(userRepository.getReferenceById(resolved))
                .badge(badge)
                .awardedBy(awardedBy)
                .awardedAt(Instant.now())
                .reason(reason)
                .build();
        return toUserBadgeResponse(userBadgeLinkRepository.save(link));
    }

    @Transactional
    public void awardBadgeSystem(Long userId, UUID badgeId, String reason) {
        awardBadge(userId, badgeId, null, reason);
    }

    private static BadgeResponse toBadgeResponse(Badge badge) {
        return BadgeResponse.builder()
                .id(badge.getId())
                .name(badge.getName())
                .description(badge.getDescription())
                .imageUrl(badge.getImageUrl())
                .createdAt(badge.getCreatedAt())
                .build();
    }

    private static UserBadgeResponse toUserBadgeResponse(UserBadgeLink link) {
        Badge badge = link.getBadge();
        return UserBadgeResponse.builder()
                .id(link.getId())
                .badgeId(badge.getId())
                .badgeName(badge.getName())
                .badgeDescription(badge.getDescription())
                .badgeImageUrl(badge.getImageUrl())
                .awardedByStaffId(link.getAwardedBy() != null ? link.getAwardedBy().getId() : null)
                .reason(link.getReason())
                .awardedAt(link.getAwardedAt())
                .build();
    }
}
