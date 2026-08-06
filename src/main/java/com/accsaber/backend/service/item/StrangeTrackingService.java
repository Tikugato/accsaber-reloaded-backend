package com.accsaber.backend.service.item;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.accsaber.backend.repository.item.UserItemLinkCounterRepository;
import com.accsaber.backend.service.player.DuplicateUserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StrangeTrackingService {

    public static final String STAT_PLAY_COUNT = "play_count";

    private static final Logger log = LoggerFactory.getLogger(StrangeTrackingService.class);

    private final UserItemLinkCounterRepository counterRepository;
    private final DuplicateUserService duplicateUserService;

    public void recordPlay(Long userId) {
        Long resolved = duplicateUserService.resolvePrimaryUserId(userId);
        int incremented = counterRepository.incrementEquippedStrange(resolved, STAT_PLAY_COUNT, 1L);
        log.debug("Strange play count bumped on {} equipped items of user {}", incremented, resolved);
    }
}
