package com.accsaber.backend.service.milestone.source;

import java.util.List;

import org.springframework.stereotype.Component;

import com.accsaber.backend.model.entity.map.Difficulty;
import com.accsaber.backend.model.entity.map.MapDifficultyStatus;

@Component
public class ScoreSources implements MilestoneSourceProvider {

    @Override
    public List<MilestoneSource> sources() {
        return List.of(scores(), scoreModifierLinks());
    }

    private MilestoneSource scores() {
        return MilestoneSource.named("scores", "scores", "s")
                .join("md", "JOIN map_difficulties {md} ON {base}.map_difficulty_id = {md}.id")
                .join("mp", "JOIN maps {mp} ON {md}.map_id = {mp}.id")
                .join("cat", "JOIN categories {cat} ON {md}.category_id = {cat}.id")
                .join("usr", "JOIN users {usr} ON {base}.user_id = {usr}.id")
                .join("sup", "JOIN scores {sup} ON {base}.supersedes_id = {sup}.id")
                .user("{base}.user_id")
                .category("{md}.category_id")
                .country("{usr}.country")
                .implicitFilter("{md}.status = 'ranked'")
                .implicitFilter("({base}.supersedes_reason IS NULL OR {base}.supersedes_reason <> 'Campaign attempt')")
                .uuid("id", "{base}.id")
                .decimal("ap", "{base}.ap")
                .decimal("weighted_ap", "{base}.weighted_ap")
                .integer("score", "{base}.score")
                .integer("score_no_mods", "{base}.score_no_mods")
                .integer("rank", "{base}.rank")
                .integer("rank_when_set", "{base}.rank_when_set")
                .integer("max_combo", "{base}.max_combo")
                .integer("misses", "{base}.misses")
                .integer("bad_cuts", "{base}.bad_cuts")
                .integer("wall_hits", "{base}.wall_hits")
                .integer("bomb_hits", "{base}.bomb_hits")
                .integer("pauses", "{base}.pauses")
                .integer("streak_115", "{base}.streak_115")
                .integer("play_count", "{base}.play_count")
                .text("hmd", "{base}.hmd")
                .flag("active", "{base}.active")
                .flag("partial", "{base}.partial")
                .decimal("xp_gained", "{base}.xp_gained")
                .flag("reweight_derivative", "{base}.reweight_derivative")
                .timestamp("time_set", "{base}.time_set")
                .uuid("supersedes_id", "{base}.supersedes_id")
                .text("supersedes_reason", "{base}.supersedes_reason")
                .timestamp("supersedes_time_set", "{sup}.time_set")
                .uuid("map_difficulty_id", "{base}.map_difficulty_id")
                .uuid("map_difficulty_uuid_id", "{base}.map_difficulty_id")
                .decimal("accuracy", "CAST({base}.score AS DOUBLE PRECISION) / {md}.max_score")
                .bigint("user_id", "{base}.user_id")
                .text("user_country", "{usr}.country")
                .flag("user_active", "{usr}.active")
                .flag("user_banned", "{usr}.banned")
                .decimal("user_total_xp", "{usr}.total_xp")
                .uuid("map_id", "{md}.map_id")
                .enumeration("map_difficulty_status", "{md}.status", MapDifficultyStatus.class)
                .text("map_difficulty_characteristic", "{md}.characteristic")
                .enumeration("map_difficulty_difficulty", "{md}.difficulty", Difficulty.class)
                .integer("map_difficulty_max_score", "{md}.max_score")
                .timestamp("map_difficulty_ranked_at", "{md}.ranked_at")
                .uuid("map_difficulty_batch_id", "{md}.batch_id")
                .uuid("map_difficulty_category_id", "{md}.category_id")
                .text("song_name", "{mp}.song_name")
                .text("song_author", "{mp}.song_author")
                .text("map_author", "{mp}.map_author")
                .text("song_hash", "{mp}.song_hash")
                .text("category_name", "{cat}.name")
                .text("category_code", "{cat}.code")
                .flag("category_count_for_overall", "{cat}.count_for_overall")
                .build();
    }

    private MilestoneSource scoreModifierLinks() {
        return MilestoneSource.named("score_modifier_links", "score_modifier_links", "sml")
                .join("mod", "JOIN modifiers {mod} ON {base}.modifier_id = {mod}.id")
                .uuid("id", "{base}.id")
                .uuid("score_id", "{base}.score_id")
                .uuid("modifier_id", "{base}.modifier_id")
                .text("modifier_code", "{mod}.code")
                .text("modifier_name", "{mod}.name")
                .build();
    }
}
