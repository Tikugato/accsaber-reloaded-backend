package com.accsaber.backend.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.accsaber.backend.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class XpRankingScheduler {

    private final UserRepository userRepository;

    @Scheduled(fixedRate = 600_000)
    public void refreshXpRankings() {
        log.debug("Refreshing XP rankings");
        try {
            userRepository.assignXpRankings();
            userRepository.assignXpCountryRankings();
        } catch (Exception e) {
            log.error("Failed to refresh XP rankings: {}", e.getMessage(), e);
        }
    }
}
