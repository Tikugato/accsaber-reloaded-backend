package com.accsaber.backend.service.player;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.accsaber.backend.exception.ConflictException;
import com.accsaber.backend.exception.ResourceNotFoundException;
import com.accsaber.backend.model.dto.response.player.UserResponse;
import com.accsaber.backend.model.entity.user.User;
import com.accsaber.backend.model.entity.user.UserNameHistory;
import com.accsaber.backend.repository.user.UserNameHistoryRepository;
import com.accsaber.backend.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserNameHistoryRepository userNameHistoryRepository;
    private final DuplicateUserService duplicateUserService;

    public UserResponse findBySteamId(Long steamId) {
        Long resolved = duplicateUserService.resolvePrimaryUserId(steamId);
        User user = userRepository.findByIdAndActiveTrue(resolved)
                .orElseThrow(() -> new ResourceNotFoundException("User", resolved));
        return toResponse(user);
    }

    public Optional<User> findOptionalBySteamId(Long steamId) {
        Long resolved = duplicateUserService.resolvePrimaryUserId(steamId);
        return userRepository.findByIdAndActiveTrue(resolved);
    }

    @Transactional
    public User createUser(Long steamId, String name, String avatarUrl, String country) {
        if (userRepository.findByIdAndActiveTrue(steamId).isPresent()) {
            throw new ConflictException("User", steamId);
        }
        return userRepository.save(User.builder()
                .id(steamId)
                .name(name)
                .avatarUrl(avatarUrl)
                .country(country)
                .build());
    }

    public BigDecimal getTotalXp(Long steamId) {
        Long resolved = duplicateUserService.resolvePrimaryUserId(steamId);
        User user = userRepository.findByIdAndActiveTrue(resolved)
                .orElseThrow(() -> new ResourceNotFoundException("User", resolved));
        return user.getTotalXp();
    }

    @Transactional
    public User updateProfile(Long steamId, String name, String avatarUrl, String country) {
        User user = userRepository.findByIdAndActiveTrue(steamId)
                .orElseThrow(() -> new ResourceNotFoundException("User", steamId));
        if (name != null && !name.equals(user.getName())) {
            userNameHistoryRepository.save(UserNameHistory.builder()
                    .user(user)
                    .name(user.getName())
                    .build());
            user.setName(name);
        }
        if (avatarUrl != null)
            user.setAvatarUrl(avatarUrl);
        if (country != null)
            user.setCountry(country);
        return userRepository.save(user);
    }

    public List<UserNameHistory> getNameHistory(Long steamId) {
        Long resolved = duplicateUserService.resolvePrimaryUserId(steamId);
        return userNameHistoryRepository.findByUser_IdOrderByChangedAtDesc(resolved);
    }

    private static UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(String.valueOf(user.getId()))
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .country(user.getCountry())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
