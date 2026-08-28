package com.accsaber.backend.service.milestone.source;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class PlayerSources implements MilestoneSourceProvider {

    @Override
    public List<MilestoneSource> sources() {
        return List.of(users(), userCategoryStatistics());
    }

    private MilestoneSource users() {
        return MilestoneSource.named("users", "users", "u")
                .user("{base}.id")
                .country("{base}.country")
                .text("name", "{base}.name")
                .decimal("total_xp", "{base}.total_xp")
                .bigint("item_essence", "{base}.item_essence")
                .bigint("net_worth", "{base}.item_essence + (SELECT COALESCE(SUM(itm.worth * lnk.quantity), 0)"
                        + " FROM user_item_links lnk JOIN items itm ON itm.id = lnk.item_id WHERE lnk.user_id = {base}.id)")
                .flag("active", "{base}.active")
                .flag("banned", "{base}.banned")
                .text("country", "{base}.country")
                .build();
    }

    private MilestoneSource userCategoryStatistics() {
        return MilestoneSource.named("user_category_statistics", "user_category_statistics", "ucs")
                .join("cat", "JOIN categories {cat} ON {base}.category_id = {cat}.id")
                .join("usr", "JOIN users {usr} ON {base}.user_id = {usr}.id")
                .user("{base}.user_id")
                .category("{base}.category_id")
                .country("{usr}.country")
                .uuid("id", "{base}.id")
                .decimal("ap", "{base}.ap")
                .decimal("average_acc", "{base}.average_acc")
                .decimal("average_ap", "{base}.average_ap")
                .integer("ranked_plays", "{base}.ranked_plays")
                .integer("ranking", "{base}.ranking")
                .integer("country_ranking", "{base}.country_ranking")
                .flag("active", "{base}.active")
                .uuid("category_id", "{base}.category_id")
                .text("category_name", "{cat}.name")
                .text("category_code", "{cat}.code")
                .flag("category_count_for_overall", "{cat}.count_for_overall")
                .bigint("user_id", "{base}.user_id")
                .text("user_country", "{usr}.country")
                .flag("user_active", "{usr}.active")
                .flag("user_banned", "{usr}.banned")
                .decimal("user_total_xp", "{usr}.total_xp")
                .build();
    }
}
