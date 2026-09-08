package com.accsaber.backend.service.mission;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.accsaber.backend.model.entity.Category;
import com.accsaber.backend.model.entity.user.UserCategorySkill;
import com.accsaber.backend.repository.score.ScoreRepository;

class MissionSkillServiceTest {

    private final MissionSkillService service = new MissionSkillService(Mockito.mock(ScoreRepository.class));

    private static Category category(String code) {
        Category c = new Category();
        c.setId(UUID.randomUUID());
        c.setCode(code);
        return c;
    }

    private static UserCategorySkill skill(Category category, Double threshold) {
        return UserCategorySkill.builder()
                .category(category)
                .skillLevel(50)
                .rawApForOneGain(threshold)
                .build();
    }

    private MissionAssignmentContext contextOf(UserCategorySkill... skills) {
        Map<UUID, UserCategorySkill> byCategory = new java.util.HashMap<>();
        for (UserCategorySkill s : skills) {
            byCategory.put(s.getCategory().getId(), s);
        }
        return new MissionAssignmentContext(1L, List.of(), byCategory, Map.of(), 100.0);
    }

    @Test
    @DisplayName("a category mission uses that category's threshold")
    void categoryMissionUsesItsOwnThreshold() {
        Category trueAcc = category("true_acc");
        Category tech = category("tech_acc");

        Double threshold = service.skillThresholdFor(
                contextOf(skill(trueAcc, 800.0), skill(tech, 600.0)), trueAcc);

        assertThat(threshold).isEqualTo(800.0);
    }

    @Test
    @DisplayName("an overall mission averages the real categories")
    void overallMissionAveragesCategories() {
        Category overall = category("overall");
        MissionAssignmentContext ctx = contextOf(
                skill(category("true_acc"), 800.0),
                skill(category("tech_acc"), 600.0),
                skill(overall, null));

        assertThat(service.skillThresholdFor(ctx, overall)).isEqualTo(700.0);
        assertThat(service.skillThresholdFor(ctx, null)).isEqualTo(700.0);
    }

    @Test
    @DisplayName("a category with no threshold of its own falls back to the average")
    void missingCategoryThresholdFallsBack() {
        Category lowMid = category("low_mid");
        MissionAssignmentContext ctx = contextOf(
                skill(category("true_acc"), 900.0),
                skill(category("standard"), 700.0),
                skill(lowMid, null));

        assertThat(service.skillThresholdFor(ctx, lowMid)).isEqualTo(800.0);
    }

    @Test
    @DisplayName("a player with no thresholds anywhere gets nothing rather than a zero")
    void noThresholdsAnywhere() {
        Category overall = category("overall");

        assertThat(service.skillThresholdFor(contextOf(skill(overall, null)), overall)).isNull();
    }
}
