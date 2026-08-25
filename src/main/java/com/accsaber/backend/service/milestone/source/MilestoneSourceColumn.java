package com.accsaber.backend.service.milestone.source;

public record MilestoneSourceColumn(String key, String template, MilestoneValueType type,
        Class<? extends Enum<?>> enumClass) {

    public static MilestoneSourceColumn of(String key, String template, MilestoneValueType type) {
        return new MilestoneSourceColumn(key, template, type, null);
    }

    public static MilestoneSourceColumn enumOf(String key, String template, Class<? extends Enum<?>> enumClass) {
        return new MilestoneSourceColumn(key, template, MilestoneValueType.ENUM, enumClass);
    }
}
