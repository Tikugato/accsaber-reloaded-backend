package com.accsaber.backend.model.dto.response.campaign;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.accsaber.backend.model.dto.response.staff.PublicStaffUserResponse;
import com.accsaber.backend.model.entity.campaign.CampaignBackgroundPlacement;
import com.accsaber.backend.model.entity.campaign.CampaignCompletionMode;
import com.accsaber.backend.model.entity.campaign.CampaignStatus;
import com.accsaber.backend.model.entity.campaign.CampaignVoteDirection;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CampaignDetailResponse {

    private UUID id;
    private String creatorId;
    private String creatorName;
    private String creatorAlias;
    private String name;
    private String slug;
    private String summary;
    private String description;
    private CampaignStatus status;
    private boolean official;
    private boolean progressionAgnostic;
    private CampaignCompletionMode completionMode;
    private boolean legacy;
    private BigDecimal completionXp;
    private String curatorNotes;
    private boolean playlistExportEnabled;
    private String backgroundUrl;
    private String backgroundColor;
    private CampaignBackgroundPlacement background;
    private String iconUrl;
    private int totalUpvotes;
    private int totalDownvotes;
    private double voteScore;
    private CampaignVoteDirection myVote;
    private boolean loved;
    private Instant lovedAt;
    private PublicStaffUserResponse lovedBy;
    private Instant curatedAt;
    private PublicStaffUserResponse curatedBy;
    private Instant publishedAt;
    private Instant createdAt;
    private List<CampaignTagResponse> tags;
    private List<CampaignDifficultyResponse> difficulties;
    private List<CampaignBarrierResponse> barriers;
    private List<CampaignTextResponse> texts;
    private List<CampaignItemAwardResponse> completionItems;
}
