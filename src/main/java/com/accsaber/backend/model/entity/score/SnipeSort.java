package com.accsaber.backend.model.entity.score;

public enum SnipeSort {
    GAP("((s_b.score - s_a.score) * 1.0 / d.maxScore)", false, true, "closest gap"),
    AP_GAP("(s_b.ap - s_a.ap)", true, false, "AP gap"),
    TARGET_AP("s_b.ap", true, false, "their AP"),
    YOUR_AP("s_a.ap", true, false, "your AP"),
    RANK_GAP("(s_a.rank - s_b.rank)", true, false, "rank gap");

    private final String expression;
    private final boolean descendingByDefault;
    private final boolean nullable;
    private final String label;

    SnipeSort(String expression, boolean descendingByDefault, boolean nullable, String label) {
        this.expression = expression;
        this.descendingByDefault = descendingByDefault;
        this.nullable = nullable;
        this.label = label;
    }

    public String getExpression() {
        return expression;
    }

    public boolean isDescendingByDefault() {
        return descendingByDefault;
    }

    public boolean isNullable() {
        return nullable;
    }

    public String getLabel() {
        return label;
    }

    public String getSlug() {
        return name().toLowerCase().replace('_', '-');
    }
}
