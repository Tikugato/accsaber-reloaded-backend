package com.accsaber.backend.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.accsaber.backend.model.entity.map.MapDifficultyMetadata;

public final class MapDifficultyMetrics {

    private MapDifficultyMetrics() {
    }

    public static Integer maxCombo(MapDifficultyMetadata metadata) {
        if (metadata == null || metadata.getNotes() == null) {
            return null;
        }
        return metadata.getNotes();
    }

    public static BigDecimal nps(MapDifficultyMetadata metadata) {
        if (metadata == null || metadata.getNotes() == null || metadata.getDuration() == null
                || metadata.getDuration() <= 0) {
            return null;
        }
        return BigDecimal.valueOf(metadata.getNotes())
                .divide(BigDecimal.valueOf(metadata.getDuration()), 2, RoundingMode.HALF_UP);
    }
}
