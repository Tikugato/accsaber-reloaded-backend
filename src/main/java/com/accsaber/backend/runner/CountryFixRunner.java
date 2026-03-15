package com.accsaber.backend.runner;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.accsaber.backend.client.ScoreSaberClient;
import com.accsaber.backend.model.dto.platform.scoresaber.ScoreSaberPlayerResponse;
import com.accsaber.backend.model.entity.user.User;
import com.accsaber.backend.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
public class CountryFixRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final ScoreSaberClient scoreSaberClient;

    @Override
    public void run(ApplicationArguments args) {
        List<User> users = userRepository.findByCountryAndActiveTrue("not set");
        if (users.isEmpty()) {
            log.info("No users with 'not set' country — skipping fix");
            return;
        }

        log.info("Fixing country for {} users with 'not set'", users.size());
        int fixed = 0;
        int failed = 0;

        for (User user : users) {
            try {
                String steamId = String.valueOf(user.getId());
                String country = scoreSaberClient.getPlayer(steamId)
                        .map(ScoreSaberPlayerResponse::getCountry)
                        .orElse(null);

                if (country != null && !country.isBlank()) {
                    user.setCountry(country);
                    userRepository.save(user);
                    fixed++;
                } else {
                    failed++;
                    log.debug("No SS country found for user {}", user.getId());
                }

                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Country fix interrupted");
                return;
            } catch (Exception e) {
                failed++;
                log.error("Failed to fix country for user {}: {}", user.getId(), e.getMessage());
            }
        }

        log.info("Country fix complete: {} fixed, {} could not be resolved", fixed, failed);
    }
}
