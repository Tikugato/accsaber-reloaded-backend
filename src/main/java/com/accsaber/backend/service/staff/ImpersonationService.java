package com.accsaber.backend.service.staff;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.accsaber.backend.exception.ValidationException;
import com.accsaber.backend.model.dto.response.PlayerAuthResponse;
import com.accsaber.backend.model.entity.staff.StaffUser;
import com.accsaber.backend.model.entity.staff.StaffUserStatus;
import com.accsaber.backend.model.entity.user.User;
import com.accsaber.backend.repository.staff.StaffUserRepository;
import com.accsaber.backend.repository.user.UserRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@ConditionalOnProperty(name = "accsaber.impersonation.enabled", havingValue = "true")
@RequiredArgsConstructor
public class ImpersonationService {

    private final UserRepository userRepository;
    private final StaffUserRepository staffUserRepository;
    private final JwtService jwtService;
    private final Environment environment;

    @Value("${accsaber.impersonation.token-ttl:3600}")
    private long tokenTtl;

    @PostConstruct
    void refuseToRunOutsideStaging() {
        boolean staging = false;
        for (String profile : environment.getActiveProfiles()) {
            staging |= "staging".equalsIgnoreCase(profile);
        }
        if (!staging) {
            throw new IllegalStateException(
                    "accsaber.impersonation.enabled is true but the staging profile is not active. This endpoint "
                            + "mints a player token for any account, so it is only ever allowed on staging. "
                            + "Refusing to start.");
        }
        log.warn("Player impersonation is ENABLED on this instance.");
    }

    public PlayerAuthResponse impersonate(Long userId, String actingStaff) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("No such user: " + userId));

        log.warn("IMPERSONATION: staff {} is acting as user {} ({})", actingStaff, user.getId(), user.getName());

        return PlayerAuthResponse.builder()
                .accessToken(jwtService.generatePlayerAccessToken(user.getId(), "impersonation"))
                .expiresIn(tokenTtl)
                .userId(String.valueOf(user.getId()))
                .roles(staffUserRepository
                        .findByUserIdAndStatusAndActiveTrue(user.getId(), StaffUserStatus.ACCEPTED)
                        .stream()
                        .map(StaffUser::getRole)
                        .distinct()
                        .sorted()
                        .toList())
                .build();
    }
}
