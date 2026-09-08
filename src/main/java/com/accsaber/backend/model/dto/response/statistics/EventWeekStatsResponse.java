package com.accsaber.backend.model.dto.response.statistics;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventWeekStatsResponse {

    private int week;
    private long missionsAssigned;
    private long missionsCompleted;
    private long missionsExpired;
    private long missionsOpen;
    private Double completionRate;
    private long xpPaid;
    private long participantsReached;
    private long participantsStoppedHere;
}
