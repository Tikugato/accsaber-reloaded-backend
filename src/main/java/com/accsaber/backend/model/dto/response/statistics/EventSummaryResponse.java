package com.accsaber.backend.model.dto.response.statistics;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventSummaryResponse {

    private UUID eventId;
    private String title;
    private String slug;
    private Instant startsAt;
    private Instant endsAt;
    private long daysRan;
    private int totalWeeks;
    private Integer week;

    private long participants;
    private long finishers;
    private Double finishRate;
    private Double averageMissionsCompleted;
    private Double medianMissionsCompleted;

    private long bonusXpPaid;
    private long missionXpPaid;
    private long totalXpPaid;

    private long missionsAssigned;
    private long missionsCompleted;
    private long missionsExpired;
    private long missionsOpen;
    private Double missionCompletionRate;

    private List<EventWeekStatsResponse> weeks;
    private List<MissionCalibrationResponse> missions;
}
