package com.accsaber.backend.service.milestone.source;

import java.util.List;

import org.springframework.stereotype.Component;

import com.accsaber.backend.model.entity.campaign.CampaignStatus;
import com.accsaber.backend.model.entity.campaign.UserCampaignStatus;
import com.accsaber.backend.model.entity.mission.MissionBand;
import com.accsaber.backend.model.entity.mission.MissionPool;

@Component
public class ProgressSources implements MilestoneSourceProvider {

    @Override
    public List<MilestoneSource> sources() {
        return List.of(userCampaigns(), userCampaignScores(), userMissions());
    }

    private MilestoneSource userCampaigns() {
        return MilestoneSource.named("user_campaigns", "user_campaigns", "ucp")
                .triggeredBy(MilestoneTrigger.CAMPAIGN)
                .join("cmp", "JOIN campaigns {cmp} ON {base}.campaign_id = {cmp}.id")
                .user("{base}.user_id")
                .uuid("id", "{base}.id")
                .uuid("campaign_id", "{base}.campaign_id")
                .enumeration("status", "{base}.status", UserCampaignStatus.class)
                .timestamp("started_at", "{base}.started_at")
                .timestamp("completed_at", "{base}.completed_at")
                .text("campaign_name", "{cmp}.name")
                .enumeration("campaign_status", "{cmp}.status", CampaignStatus.class)
                .flag("campaign_official", "{cmp}.official")
                .flag("campaign_loved", "{cmp}.loved")
                .build();
    }

    private MilestoneSource userCampaignScores() {
        return MilestoneSource.named("user_campaign_scores", "user_campaign_scores", "ucs2")
                .triggeredBy(MilestoneTrigger.CAMPAIGN)
                .join("cmp", "JOIN campaigns {cmp} ON {base}.campaign_id = {cmp}.id")
                .user("{base}.user_id")
                .uuid("id", "{base}.id")
                .uuid("campaign_id", "{base}.campaign_id")
                .uuid("campaign_difficulty_id", "{base}.campaign_difficulty_id")
                .timestamp("submitted_at", "{base}.submitted_at")
                .text("campaign_name", "{cmp}.name")
                .enumeration("campaign_status", "{cmp}.status", CampaignStatus.class)
                .build();
    }

    private MilestoneSource userMissions() {
        return MilestoneSource.named("user_missions", "user_missions", "umi")
                .triggeredBy(MilestoneTrigger.MISSION)
                .join("cat", "JOIN categories {cat} ON {base}.category_id = {cat}.id")
                .user("{base}.user_id")
                .category("{base}.category_id")
                .uuid("id", "{base}.id")
                .uuid("template_id", "{base}.template_id")
                .enumeration("pool", "{base}.pool", MissionPool.class)
                .enumeration("band", "{base}.band", MissionBand.class)
                .integer("progress_count", "{base}.progress_count")
                .integer("target_count", "{base}.target_count")
                .integer("xp_reward", "{base}.xp_reward")
                .flag("item_awarded", "{base}.item_awarded")
                .timestamp("assigned_at", "{base}.assigned_at")
                .timestamp("completed_at", "{base}.completed_at")
                .text("category_code", "{cat}.code")
                .build();
    }
}
