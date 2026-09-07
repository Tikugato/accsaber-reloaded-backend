package com.accsaber.backend.model.dto.response.campaign;

import java.util.List;
import java.util.UUID;

import com.accsaber.backend.model.entity.campaign.CampaignLabelPosition;
import com.accsaber.backend.model.entity.campaign.CampaignNodeBorderLayer;
import com.accsaber.backend.model.entity.campaign.CampaignPrerequisiteMode;
import com.accsaber.backend.model.entity.campaign.CampaignRequirementType;
import com.accsaber.backend.model.entity.map.Difficulty;
import com.accsaber.backend.model.entity.map.MapDifficultyMetadata;
import com.accsaber.backend.model.entity.map.MapDifficultyStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CampaignDifficultyResponse {

    private UUID id;
    private UUID mapDifficultyId;
    private UUID mapId;
    private UUID categoryId;
    private Double complexity;
    private String beatsaverCode;
    private Integer maxScore;
    private MapDifficultyMetadata metadata;
    private Double nps;
    private Integer maxCombo;
    private String songName;
    private String songSubName;
    private String songAuthor;
    private String mapAuthor;
    private String coverUrl;
    private String cdnCoverUrl;
    private Difficulty difficulty;
    private String characteristic;
    private MapDifficultyStatus mapDifficultyStatus;
    private CampaignRequirementType requirementType;
    private Double requirementValue;
    private Double requirementValueMax;
    private CampaignPrerequisiteMode targetMode;
    private List<CampaignTargetResponse> targets;
    private CampaignPrerequisiteMode prerequisiteMode;
    private boolean terminal;
    private String description;
    private String checkpointLabel;
    private CampaignLabelPosition checkpointLabelPosition;
    private String checkpointAvatarUrl;
    private String checkpointColor;
    private String borderColor;
    private String borderShape;
    private String nodeBorderUrl;
    private CampaignNodeBorderLayer nodeBorderLayer;
    private Integer size;
    private Integer checkpointSize;
    private Double positionX;
    private Double positionY;
    private Double xp;
    private List<CampaignConnectionResponse> prerequisites;
    private List<CampaignItemAwardResponse> items;
    private List<CampaignModifierRequirementResponse> modifiers;
}
