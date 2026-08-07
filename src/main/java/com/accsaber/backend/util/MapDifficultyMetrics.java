package com.accsaber.backend.util;

import com.accsaber.backend.util.Rounding;

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

    public static Double nps(MapDifficultyMetadata metadata) {
        if (metadata == null || metadata.getNotes() == null || metadata.getDuration() == null
                || metadata.getDuration() <= 0) {
            return null;
        }
        return Rounding.round((double) (metadata.getNotes()) / (double) (metadata.getDuration()), 2);
    }
}
