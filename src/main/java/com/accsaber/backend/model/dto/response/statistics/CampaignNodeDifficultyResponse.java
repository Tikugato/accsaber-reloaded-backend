package com.accsaber.backend.model.dto.response.statistics;

import java.util.UUID;

import com.accsaber.backend.model.entity.campaign.CampaignRequirementType;
import com.accsaber.backend.model.entity.map.Difficulty;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CampaignNodeDifficultyResponse {

    private UUID campaignDifficultyId;
    private UUID mapDifficultyId;
    private String songName;
    private String songSubName;
    private String songAuthor;
    private String mapAuthor;
    private String coverUrl;
    private String cdnCoverUrl;
    private Difficulty difficulty;
    private boolean barrier;
    private boolean terminal;
    private CampaignRequirementType requirementType;
    private Double requirementValue;
    private double xp;
    private long unlocked;
    private long cleared;
    private Double clearRate;
    private Double medianDaysToClear;
}
