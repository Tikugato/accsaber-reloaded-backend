package com.accsaber.backend.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import com.accsaber.backend.model.dto.projection.ScoreModifierRow;
import com.accsaber.backend.model.entity.score.Score;

public record ScoreModifierIndex(Map<UUID, Set<UUID>> modifierIdsByScore, Set<UUID> nfScoreIds) {

    private static final String NO_FAIL_CODE = "NF";
    private static final ScoreModifierIndex EMPTY = new ScoreModifierIndex(Map.of(), Set.of());

    public static ScoreModifierIndex load(Collection<Score> scores,
            Function<Collection<UUID>, List<ScoreModifierRow>> fetch) {
        List<UUID> ids = scores.stream().map(Score::getId).filter(Objects::nonNull).toList();
        return ids.isEmpty() ? EMPTY : of(fetch.apply(ids));
    }

    private static ScoreModifierIndex of(Collection<ScoreModifierRow> rows) {
        if (rows.isEmpty()) {
            return EMPTY;
        }
        Map<UUID, Set<UUID>> modifierIdsByScore = new HashMap<>();
        Set<UUID> nfScoreIds = new HashSet<>();
        for (ScoreModifierRow row : rows) {
            modifierIdsByScore.computeIfAbsent(row.scoreId(), k -> new HashSet<>()).add(row.modifierId());
            if (NO_FAIL_CODE.equals(row.code())) {
                nfScoreIds.add(row.scoreId());
            }
        }
        return new ScoreModifierIndex(modifierIdsByScore, nfScoreIds);
    }

    public boolean hasNoFail(UUID scoreId) {
        return nfScoreIds.contains(scoreId);
    }

    private Set<UUID> modifiersOf(UUID scoreId) {
        return modifierIdsByScore.getOrDefault(scoreId, Set.of());
    }

    public boolean satisfies(UUID scoreId, CampaignModifierRule rule) {
        return rule == null || rule.isEmpty() || rule.matches(modifiersOf(scoreId));
    }
}
