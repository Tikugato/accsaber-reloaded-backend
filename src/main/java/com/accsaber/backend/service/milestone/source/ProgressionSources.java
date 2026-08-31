package com.accsaber.backend.service.milestone.source;

import java.util.List;

import org.springframework.stereotype.Component;

import com.accsaber.backend.model.entity.milestone.MilestoneStatus;

@Component
public class ProgressionSources implements MilestoneSourceProvider {

    @Override
    public List<MilestoneSource> sources() {
        return List.of(userMilestoneLinks(), milestones(), milestoneSets(), levelThresholds(), categories(),
                modifiers());
    }

    private MilestoneSource userMilestoneLinks() {
        return MilestoneSource.named("user_milestone_links", "user_milestone_links", "uml")
                .join("usr", "JOIN users {usr} ON {base}.user_id = {usr}.id")
                .join("mil", "JOIN milestones {mil} ON {base}.milestone_id = {mil}.id")
                .user("{base}.user_id")
                .country("{usr}.country")
                .uuid("id", "{base}.id")
                .flag("completed", "{base}.completed")
                .decimal("progress", "{base}.progress")
                .timestamp("completed_at", "{base}.completed_at")
                .bigint("user_id", "{base}.user_id")
                .text("user_country", "{usr}.country")
                .flag("user_active", "{usr}.active")
                .flag("user_banned", "{usr}.banned")
                .uuid("milestone_id", "{base}.milestone_id")
                .decimal("milestone_xp", "{mil}.xp")
                .decimal("milestone_target_value", "{mil}.target_value")
                .text("milestone_type", "{mil}.type")
                .text("milestone_tier", "{mil}.tier")
                .uuid("milestone_set_id", "{mil}.set_id")
                .build();
    }

    private MilestoneSource milestones() {
        return MilestoneSource.named("milestones", "milestones", "mil")
                .uuid("id", "{base}.id")
                .text("title", "{base}.title")
                .text("type", "{base}.type")
                .text("tier", "{base}.tier")
                .decimal("xp", "{base}.xp")
                .decimal("target_value", "{base}.target_value")
                .flag("active", "{base}.active")
                .enumeration("status", "{base}.status", MilestoneStatus.class)
                .text("comparison", "{base}.comparison")
                .uuid("set_id", "{base}.set_id")
                .build();
    }

    private MilestoneSource milestoneSets() {
        return MilestoneSource.named("milestone_sets", "milestone_sets", "mset")
                .uuid("id", "{base}.id")
                .text("title", "{base}.title")
                .decimal("set_bonus_xp", "{base}.set_bonus_xp")
                .flag("active", "{base}.active")
                .build();
    }

    private MilestoneSource levelThresholds() {
        return MilestoneSource.named("level_thresholds", "level_thresholds", "lt")
                .integer("level", "{base}.level")
                .text("title", "{base}.title")
                .build();
    }

    private MilestoneSource categories() {
        return MilestoneSource.named("categories", "categories", "cat")
                .uuid("id", "{base}.id")
                .text("name", "{base}.name")
                .text("code", "{base}.code")
                .flag("count_for_overall", "{base}.count_for_overall")
                .flag("active", "{base}.active")
                .build();
    }

    private MilestoneSource modifiers() {
        return MilestoneSource.named("modifiers", "modifiers", "mod")
                .uuid("id", "{base}.id")
                .text("name", "{base}.name")
                .text("code", "{base}.code")
                .decimal("multiplier", "{base}.multiplier")
                .build();
    }
}
