package com.accsaber.backend.model.dto.request.campaign;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import com.accsaber.backend.model.entity.campaign.CampaignLabelPosition;
import com.accsaber.backend.model.entity.campaign.CampaignNodeBorderLayer;
import com.accsaber.backend.model.entity.campaign.CampaignPrerequisiteMode;
import com.accsaber.backend.model.entity.campaign.CampaignRequirementType;
import com.accsaber.backend.validation.CleanText;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCampaignDifficultyRequest {

    private CampaignRequirementType requirementType;
    private CampaignPrerequisiteMode prerequisiteMode;
    private BigDecimal requirementValue;
    private BigDecimal requirementValueMax;
    private Set<CampaignBound> clear;

    @Valid
    @Size(max = 8)
    private List<CampaignTargetRequest> targets;

    private CampaignPrerequisiteMode targetMode;

    @Size(max = 1000)
    @CleanText
    private String description;

    @Size(max = 80)
    @CleanText
    private String checkpointLabel;

    private CampaignLabelPosition checkpointLabelPosition;

    @Size(max = 512)
    @Pattern(regexp = "^$|^https?://[^\\s\"'<>]+$", message = "must be a valid http(s) URL")
    private String checkpointAvatarUrl;

    @Pattern(regexp = "^$|^#?[A-Za-z0-9]{1,32}$", message = "must be a hex or named color")
    private String checkpointColor;

    @Pattern(regexp = "^$|^#?[A-Za-z0-9]{1,32}$", message = "must be a hex or named color")
    private String borderColor;

    @Pattern(regexp = "^$|^[A-Za-z0-9 _-]{1,32}$", message = "invalid style token")
    private String borderShape;

    @Size(max = 512)
    @Pattern(regexp = "^$|^https?://[^\\s\"'<>]+$", message = "must be a valid http(s) URL")
    private String nodeBorderUrl;

    private CampaignNodeBorderLayer nodeBorderLayer;

    @PositiveOrZero
    private Integer size;

    @PositiveOrZero
    private Integer checkpointSize;

    private BigDecimal positionX;
    private BigDecimal positionY;
    private BigDecimal xp;

    @Valid
    @Size(max = 25)
    private List<CampaignConnectionRequest> prerequisites;

    @Valid
    @Size(max = 20)
    private List<CampaignModifierRequirementRequest> modifiers;
}
