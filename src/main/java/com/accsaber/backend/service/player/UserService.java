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

    public UserResponse findByUserId(Long userId) {
        Long resolved = duplicateUserService.resolvePrimaryUserId(userId);
        User user = userRepository.findByIdAndActiveTrue(resolved)
                .orElseThrow(() -> new ResourceNotFoundException("User", resolved));
        return toResponse(user);
    }

    public Optional<User> findOptionalByUserId(Long userId) {
        Long resolved = duplicateUserService.resolvePrimaryUserId(userId);
        return userRepository.findByIdAndActiveTrue(resolved);
    }

    @Transactional
    public User createUser(Long userId, String name, String avatarUrl, String country) {
        if (userRepository.findByIdAndActiveTrue(userId).isPresent()) {
            throw new ConflictException("User", userId);
        }
        return userRepository.save(User.builder()
                .id(userId)
                .name(name)
                .avatarUrl(avatarUrl)
                .country(country)
                .build());
    }

    public BigDecimal getTotalXp(Long userId) {
        Long resolved = duplicateUserService.resolvePrimaryUserId(userId);
        User user = userRepository.findByIdAndActiveTrue(resolved)
                .orElseThrow(() -> new ResourceNotFoundException("User", resolved));
        return user.getTotalXp();
    }

    @Transactional
    public User updateProfile(Long userId, String name, String avatarUrl, String country) {
        User user = userRepository.findByIdAndActiveTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
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

    public List<UserNameHistory> getNameHistory(Long userId) {
        Long resolved = duplicateUserService.resolvePrimaryUserId(userId);
        return userNameHistoryRepository.findByUser_IdOrderByChangedAtDesc(resolved);
    }

    private static UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(String.valueOf(user.getId()))
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .country(user.getCountry())
                .xpRanking(user.getXpRanking())
                .xpCountryRanking(user.getXpCountryRanking())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
