package com.accsaber.backend.service.milestone.source;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MilestoneSource {

    public static final String BASE = "base";

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\w+)}");

    private final String name;
    private final String table;
    private final String baseAlias;
    private final Map<String, String> joins;
    private final Map<String, MilestoneSourceColumn> columns;
    private final String userTemplate;
    private final String categoryTemplate;
    private final String countryTemplate;
    private final List<String> implicitFilters;
    private final Set<MilestoneTrigger> triggers;

    private MilestoneSource(Builder builder) {
        this.name = builder.name;
        this.table = builder.table;
        this.baseAlias = builder.baseAlias;
        this.joins = Map.copyOf(builder.joins);
        this.columns = new LinkedHashMap<>(builder.columns);
        this.userTemplate = builder.userTemplate;
        this.categoryTemplate = builder.categoryTemplate;
        this.countryTemplate = builder.countryTemplate;
        this.implicitFilters = List.copyOf(builder.implicitFilters);
        this.triggers = Set.copyOf(builder.triggers);
        validate();
    }

    private void validate() {
        for (MilestoneSourceColumn column : columns.values()) {
            assertResolvable(column.template(), "column '" + column.key() + "'");
        }
        for (Map.Entry<String, String> join : joins.entrySet()) {
            assertResolvable(join.getValue(), "join '" + join.getKey() + "'");
        }
        for (String filter : implicitFilters) {
            assertResolvable(filter, "implicit filter");
        }
        assertResolvable(userTemplate, "user path");
        assertResolvable(categoryTemplate, "category path");
        assertResolvable(countryTemplate, "country path");
    }

    private void assertResolvable(String template, String what) {
        if (template == null) {
            return;
        }
        for (String placeholder : placeholders(template)) {
            if (!BASE.equals(placeholder) && !joins.containsKey(placeholder)) {
                throw new IllegalStateException(
                        "Milestone source '" + name + "' " + what + " references unknown join '" + placeholder + "'");
            }
        }
    }

    public static List<String> placeholders(String template) {
        List<String> found = new ArrayList<>();
        Matcher matcher = PLACEHOLDER.matcher(template);
        while (matcher.find()) {
            found.add(matcher.group(1));
        }
        return found;
    }

    public String name() {
        return name;
    }

    public String table() {
        return table;
    }

    public String baseAlias() {
        return baseAlias;
    }

    public String joinTemplate(String key) {
        return joins.get(key);
    }

    public Map<String, MilestoneSourceColumn> columns() {
        return columns;
    }

    public MilestoneSourceColumn column(String key) {
        return columns.get(key);
    }

    public boolean hasColumn(String key) {
        return columns.containsKey(key);
    }

    public String userTemplate() {
        return userTemplate;
    }

    public String categoryTemplate() {
        return categoryTemplate;
    }

    public String countryTemplate() {
        return countryTemplate;
    }

    public List<String> implicitFilters() {
        return implicitFilters;
    }

    public Set<MilestoneTrigger> triggers() {
        return triggers;
    }

    public static Builder named(String name, String table, String baseAlias) {
        return new Builder(name, table, baseAlias);
    }

    public static final class Builder {

        private final String name;
        private final String table;
        private final String baseAlias;
        private final Map<String, String> joins = new LinkedHashMap<>();
        private final Map<String, MilestoneSourceColumn> columns = new LinkedHashMap<>();
        private final List<String> implicitFilters = new ArrayList<>();
        private final Set<MilestoneTrigger> triggers = EnumSet.of(MilestoneTrigger.SCORE);
        private String userTemplate;
        private String categoryTemplate;
        private String countryTemplate;

        private Builder(String name, String table, String baseAlias) {
            this.name = name;
            this.table = table;
            this.baseAlias = baseAlias;
        }

        public Builder join(String key, String template) {
            joins.put(key, template);
            return this;
        }

        public Builder text(String key, String template) {
            return column(MilestoneSourceColumn.of(key, template, MilestoneValueType.STRING));
        }

        public Builder integer(String key, String template) {
            return column(MilestoneSourceColumn.of(key, template, MilestoneValueType.INTEGER));
        }

        public Builder bigint(String key, String template) {
            return column(MilestoneSourceColumn.of(key, template, MilestoneValueType.LONG));
        }

        public Builder decimal(String key, String template) {
            return column(MilestoneSourceColumn.of(key, template, MilestoneValueType.DOUBLE));
        }

        public Builder flag(String key, String template) {
            return column(MilestoneSourceColumn.of(key, template, MilestoneValueType.BOOLEAN));
        }

        public Builder timestamp(String key, String template) {
            return column(MilestoneSourceColumn.of(key, template, MilestoneValueType.INSTANT));
        }

        public Builder uuid(String key, String template) {
            return column(MilestoneSourceColumn.of(key, template, MilestoneValueType.UUID));
        }

        public Builder enumeration(String key, String template, Class<? extends Enum<?>> enumClass) {
            return column(MilestoneSourceColumn.enumOf(key, template, enumClass));
        }

        private Builder column(MilestoneSourceColumn column) {
            columns.put(column.key(), column);
            return this;
        }

        public Builder user(String template) {
            this.userTemplate = template;
            return this;
        }

        public Builder category(String template) {
            this.categoryTemplate = template;
            return this;
        }

        public Builder country(String template) {
            this.countryTemplate = template;
            return this;
        }

        public Builder triggeredBy(MilestoneTrigger... values) {
            triggers.clear();
            triggers.addAll(List.of(values));
            return this;
        }

        public Builder implicitFilter(String template) {
            implicitFilters.add(template);
            return this;
        }

        public MilestoneSource build() {
            return new MilestoneSource(this);
        }
    }
}
