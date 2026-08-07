package com.accsaber.backend.model.dto.response.player;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XpLeaderboardResponse {

    private Integer ranking;
    private Integer countryRanking;
    private String userId;
    private String userName;
    private String country;
    private String avatarUrl;
    private String cdnAvatarUrl;
    private Double totalXp;
    private Integer level;
    private boolean playerInactive;
    private Integer rankingLastWeek;
    private String supporterTier;
}
