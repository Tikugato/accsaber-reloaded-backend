package com.accsaber.backend.service.stats;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.accsaber.backend.exception.ValidationException;

public record CampaignStatsFilter(
        List<String> statuses,
        String country,
        int minParticipants) {

    private static final List<String> LIFECYCLE = List.of("published", "editing", "curated");
    private static final List<String> FLAGS = List.of("loved", "official");

    public static CampaignStatsFilter none() {
        return new CampaignStatsFilter(null, null, 0);
    }

    public String appendCampaignPredicate(Map<String, Object> params) {
        if (statuses == null || statuses.isEmpty()) {
            return "";
        }
        List<String> lifecycle = new ArrayList<>();
        List<String> clauses = new ArrayList<>();
        for (String raw : statuses) {
            String value = raw == null ? "" : raw.trim().toLowerCase();
            if (LIFECYCLE.contains(value)) {
                lifecycle.add(value);
            } else if (FLAGS.contains(value)) {
                clauses.add("c." + value + " = true");
            } else {
                throw new ValidationException("status",
                        "must be one of published, editing, curated, loved, official");
            }
        }
        if (!lifecycle.isEmpty()) {
            clauses.add("c.status IN (:lifecycle)");
            params.put("lifecycle", lifecycle);
        }
        return " AND (" + String.join(" OR ", clauses) + ")";
    }

    public String appendParticipantCountry(Map<String, Object> params) {
        return StatsQueryRunner.countryExists("uc.user_id", country, params);
    }

    public String cacheKey() {
        return statuses + "|" + country + "|" + minParticipants;
    }
}
