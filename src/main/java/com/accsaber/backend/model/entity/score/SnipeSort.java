package com.accsaber.backend.model.entity.score;

public enum SnipeSort {
    GAP("((s_b.score - s_a.score) * 1.0 / d.maxScore)", false, "closest gap"),
    AP_GAP("(s_b.ap - s_a.ap)", true, "AP gap"),
    TARGET_AP("s_b.ap", true, "their AP"),
    YOUR_AP("s_a.ap", true, "your AP"),
    RANK_GAP("(s_a.rank - s_b.rank)", true, "rank gap");

    private final String expression;
    private final boolean descendingByDefault;
    private final String label;

    SnipeSort(String expression, boolean descendingByDefault, String label) {
        this.expression = expression;
        this.descendingByDefault = descendingByDefault;
        this.label = label;
    }

    public String getExpression() {
        return expression;
    }

    public boolean isDescendingByDefault() {
        return descendingByDefault;
    }

    public String getLabel() {
        return label;
    }

    public String getSlug() {
        return name().toLowerCase().replace('_', '-');
    }
}
