package com.accsaber.backend.service.mission;

import org.springframework.stereotype.Component;

import com.accsaber.backend.model.dto.EventMissionTargets;
import com.accsaber.backend.model.entity.mission.Event;
import com.accsaber.backend.model.entity.mission.MissionTemplate;
import com.accsaber.backend.model.entity.mission.UserMission;
import com.accsaber.backend.repository.CategoryRepository;
import com.accsaber.backend.repository.map.MapDifficultyRepository;
import com.accsaber.backend.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MissionRowFactory {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final MapDifficultyRepository mapDifficultyRepository;

    public UserMission build(Long userId, MissionTemplate template, Event event) {
        UserMission.UserMissionBuilder builder = UserMission.builder()
                .user(userId != null ? userRepository.getReferenceById(userId) : null)
                .template(template)
                .pool(template.getPool())
                .xpReward(template.getFixedXp() != null ? template.getFixedXp() : 0)
                .itemReward(template.getAwardsItem())
                .expiresAt(template.closeInstant(event));
        EventMissionTargets targets = template.getEventTargets();
        if (targets != null) {
            if (targets.categoryId() != null) {
                builder.category(categoryRepository.getReferenceById(targets.categoryId()));
            }
            if (targets.mapDifficultyId() != null) {
                builder.targetMapDifficulty(mapDifficultyRepository.getReferenceById(targets.mapDifficultyId()));
            }
            if (targets.playerId() != null) {
                builder.targetPlayer(userRepository.getReferenceById(targets.playerIdAsLong()));
            }
            builder.targetAcc(targets.acc())
                    .targetAp(targets.ap())
                    .targetScore(targets.score())
                    .targetCount(targets.count())
                    .targetXp(targets.xp())
                    .targetThresholdAp(targets.thresholdAp())
                    .targetStreak(targets.streak())
                    .targetRankedBefore(targets.rankedBefore())
                    .targetCuratedOnly(targets.curatedOnly());
        }
        return builder.build();
    }
}
