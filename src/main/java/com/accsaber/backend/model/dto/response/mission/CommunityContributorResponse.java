package com.accsaber.backend.model.dto.response.mission;

import java.time.Instant;

import com.accsaber.backend.model.entity.mission.CommunityMissionContribution;
import com.accsaber.backend.model.entity.user.User;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommunityContributorResponse {

    private long rank;
    private String userId;
    private String userName;
    private String userCountry;
    private String userAvatarUrl;
    private String userCdnAvatarUrl;
    private double contribution;
    private Instant firstAt;
    private Instant lastAt;
    private Instant rewardedAt;

    public static CommunityContributorResponse from(CommunityMissionContribution c, long rank) {
        User user = c.getUser();
        return CommunityContributorResponse.builder()
                .rank(rank)
                .userId(String.valueOf(user.getId()))
                .userName(user.getName())
                .userCountry(user.getCountry())
                .userAvatarUrl(user.getAvatarUrl())
                .userCdnAvatarUrl(user.getCdnAvatarUrl())
                .contribution(c.getContribution())
                .firstAt(c.getFirstAt())
                .lastAt(c.getLastAt())
                .rewardedAt(c.getRewardedAt())
                .build();
    }
}
