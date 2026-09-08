package com.accsaber.backend.service.stats;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.accsaber.backend.model.entity.mission.MissionBand;
import com.accsaber.backend.model.entity.mission.MissionPool;
import com.accsaber.backend.model.entity.mission.MissionType;

public record MissionStatsFilter(
        List<MissionPool> pools,
        List<MissionType> types,
        UUID templateId,
        List<UUID> categoryIds,
        List<MissionBand> bands,
        List<String> tiers,
        Double skillMin,
        Double skillMax,
        String country,
        Instant from,
        Instant to,
        int minAssigned) {

    public static MissionStatsFilter none() {
        return new MissionStatsFilter(null, null, null, null, null, null, null, null, null, null, null, 1);
    }

    public String appendTo(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder();
        appendEnumIn(sql, params, "m.pool", "pools", names(pools));
        appendEnumIn(sql, params, "t.type", "types", names(types));
        if (templateId != null && !params.containsKey("templateId")) {
            sql.append(" AND m.template_id = :templateId");
            params.put("templateId", templateId);
        }
        if (notEmpty(categoryIds)) {
            sql.append(" AND m.category_id IN (:categoryIds)");
            params.put("categoryIds", categoryIds);
        }
        appendEnumIn(sql, params, "m.band", "bands", names(bands));
        if (notEmpty(tiers)) {
            sql.append(" AND ").append(MissionStatisticsService.TIER_CASE).append(" IN (:tiers)");
            params.put("tiers", tiers);
        }
        if (skillMin != null) {
            sql.append(" AND m.assigned_skill_threshold >= :skillMin");
            params.put("skillMin", skillMin);
        }
        if (skillMax != null) {
            sql.append(" AND m.assigned_skill_threshold <= :skillMax");
            params.put("skillMax", skillMax);
        }
        sql.append(StatsQueryRunner.countryExists("m.user_id", country, params));
        if (from != null) {
            sql.append(" AND m.assigned_at >= :from");
            params.put("from", from);
        }
        if (to != null) {
            sql.append(" AND m.assigned_at < :to");
            params.put("to", to);
        }
        return sql.toString();
    }

    private static void appendEnumIn(StringBuilder sql, Map<String, Object> params, String column, String name,
            List<String> values) {
        if (values.isEmpty()) {
            return;
        }
        sql.append(" AND ").append(column).append(" IN (:").append(name).append(")");
        params.put(name, values);
    }

    private static <T extends Enum<T>> List<String> names(List<T> values) {
        return notEmpty(values) ? values.stream().map(Enum::name).toList() : List.of();
    }

    private static boolean notEmpty(List<?> values) {
        return values != null && !values.isEmpty();
    }

    public String cacheKey() {
        return pools + "|" + types + "|" + templateId + "|" + categoryIds + "|" + bands + "|" + tiers
                + "|" + skillMin + "|" + skillMax + "|" + country + "|" + from + "|" + to + "|" + minAssigned;
    }
}
