package com.accsaber.backend.service.milestone.source;

import java.util.List;

import org.springframework.stereotype.Component;

import com.accsaber.backend.model.entity.map.Difficulty;
import com.accsaber.backend.model.entity.map.MapDifficultyStatus;

@Component
public class MapSources implements MilestoneSourceProvider {

    @Override
    public List<MilestoneSource> sources() {
        return List.of(maps(), mapDifficulties(), mapDifficultyStatistics(), mapDifficultyComplexities());
    }

    private MilestoneSource maps() {
        return MilestoneSource.named("maps", "maps", "mp")
                .uuid("id", "{base}.id")
                .text("song_name", "{base}.song_name")
                .text("song_author", "{base}.song_author")
                .text("map_author", "{base}.map_author")
                .text("song_hash", "{base}.song_hash")
                .flag("active", "{base}.active")
                .build();
    }

    private MilestoneSource mapDifficulties() {
        return MilestoneSource.named("map_difficulties", "map_difficulties", "md")
                .join("mp", "JOIN maps {mp} ON {base}.map_id = {mp}.id")
                .join("cat", "JOIN categories {cat} ON {base}.category_id = {cat}.id")
                .category("{base}.category_id")
                .implicitFilter("{base}.status = 'ranked'")
                .uuid("id", "{base}.id")
                .uuid("map_entity", "{base}.map_id")
                .enumeration("status", "{base}.status", MapDifficultyStatus.class)
                .integer("max_score", "{base}.max_score")
                .flag("active", "{base}.active")
                .enumeration("difficulty", "{base}.difficulty", Difficulty.class)
                .text("characteristic", "{base}.characteristic")
                .timestamp("ranked_at", "{base}.ranked_at")
                .uuid("batch_id", "{base}.batch_id")
                .uuid("map_id", "{base}.map_id")
                .text("song_name", "{mp}.song_name")
                .text("song_author", "{mp}.song_author")
                .text("map_author", "{mp}.map_author")
                .text("song_hash", "{mp}.song_hash")
                .uuid("category_id", "{base}.category_id")
                .text("category_name", "{cat}.name")
                .text("category_code", "{cat}.code")
                .flag("category_count_for_overall", "{cat}.count_for_overall")
                .build();
    }

    private MilestoneSource mapDifficultyStatistics() {
        return MilestoneSource.named("map_difficulty_statistics", "map_difficulty_statistics", "mds")
                .join("md", "JOIN map_difficulties {md} ON {base}.map_difficulty_id = {md}.id")
                .join("mp", "JOIN maps {mp} ON {md}.map_id = {mp}.id")
                .join("cat", "JOIN categories {cat} ON {md}.category_id = {cat}.id")
                .category("{md}.category_id")
                .implicitFilter("{md}.status = 'ranked'")
                .uuid("id", "{base}.id")
                .decimal("max_ap", "{base}.max_ap")
                .decimal("min_ap", "{base}.min_ap")
                .decimal("average_ap", "{base}.average_ap")
                .integer("total_scores", "{base}.total_scores")
                .flag("active", "{base}.active")
                .uuid("map_difficulty_id", "{base}.map_difficulty_id")
                .enumeration("map_difficulty_status", "{md}.status", MapDifficultyStatus.class)
                .enumeration("map_difficulty_difficulty", "{md}.difficulty", Difficulty.class)
                .text("map_difficulty_characteristic", "{md}.characteristic")
                .integer("map_difficulty_max_score", "{md}.max_score")
                .uuid("map_id", "{md}.map_id")
                .text("song_name", "{mp}.song_name")
                .text("song_hash", "{mp}.song_hash")
                .uuid("category_id", "{md}.category_id")
                .text("category_name", "{cat}.name")
                .text("category_code", "{cat}.code")
                .build();
    }

    private MilestoneSource mapDifficultyComplexities() {
        return MilestoneSource.named("map_difficulty_complexities", "map_difficulty_complexities", "mdc")
                .join("md", "JOIN map_difficulties {md} ON {base}.map_difficulty_id = {md}.id")
                .join("mp", "JOIN maps {mp} ON {md}.map_id = {mp}.id")
                .join("cat", "JOIN categories {cat} ON {md}.category_id = {cat}.id")
                .category("{md}.category_id")
                .implicitFilter("{md}.status = 'ranked'")
                .uuid("id", "{base}.id")
                .uuid("map_difficulty_uuid_id", "{base}.map_difficulty_id")
                .decimal("complexity", "{base}.complexity")
                .flag("active", "{base}.active")
                .enumeration("map_difficulty_status", "{md}.status", MapDifficultyStatus.class)
                .enumeration("map_difficulty_difficulty", "{md}.difficulty", Difficulty.class)
                .text("map_difficulty_characteristic", "{md}.characteristic")
                .integer("map_difficulty_max_score", "{md}.max_score")
                .uuid("map_id", "{md}.map_id")
                .text("song_name", "{mp}.song_name")
                .text("song_hash", "{mp}.song_hash")
                .uuid("category_id", "{md}.category_id")
                .text("category_name", "{cat}.name")
                .text("category_code", "{cat}.code")
                .build();
    }
}
