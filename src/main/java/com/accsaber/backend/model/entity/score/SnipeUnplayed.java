package com.accsaber.backend.model.entity.score;

public enum SnipeUnplayed {
    EXCLUDE("played only"),
    INCLUDE("with unplayed"),
    ONLY("unplayed only");

    private final String label;

    SnipeUnplayed(String label) {
        this.label = label;
    }

    public boolean isDefault() {
        return this == EXCLUDE;
    }

    public boolean allowsPlayed() {
        return this != ONLY;
    }

    public boolean allowsUnplayed() {
        return this != EXCLUDE;
    }

    public String getLabel() {
        return label;
    }

    public String getSlug() {
        return "unplayed-" + name().toLowerCase();
    }
}
