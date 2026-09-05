package com.accsaber.backend.service.mission;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.accsaber.backend.model.dto.response.mission.MissionResponse;
import com.accsaber.backend.model.entity.mission.UserMission;
import com.accsaber.backend.repository.mission.CommunityMissionContributionRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CommunityContextLoader {

    private final CommunityMissionContributionRepository contributionRepository;

    public MissionResponse.CommunityContext load(List<UserMission> missions, Long viewerId) {
        List<UUID> ids = missions.stream()
                .filter(UserMission::isCommunity)
                .map(UserMission::getId)
                .toList();
        if (ids.isEmpty()) {
            return MissionResponse.CommunityContext.EMPTY;
        }
        Map<UUID, Long> contributors = contributionRepository.countContributors(ids).stream()
                .collect(Collectors.toMap(
                        CommunityMissionContributionRepository.ContributorCountView::getMissionId,
                        CommunityMissionContributionRepository.ContributorCountView::getContributors));
        if (viewerId == null) {
            return new MissionResponse.CommunityContext(contributors, Map.of());
        }
        Map<UUID, Double> yours = new HashMap<>();
        for (CommunityMissionContributionRepository.ContributionView view : contributionRepository
                .findContributionsByUser(viewerId, ids)) {
            yours.put(view.getMissionId(), view.getContribution());
        }
        return new MissionResponse.CommunityContext(contributors, yours);
    }
}
