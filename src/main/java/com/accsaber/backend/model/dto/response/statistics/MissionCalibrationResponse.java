package com.accsaber.backend.model.dto.response.statistics;

import java.util.UUID;

import com.accsaber.backend.model.entity.mission.MissionBand;
import com.accsaber.backend.model.entity.mission.MissionPool;
import com.accsaber.backend.model.entity.mission.MissionType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MissionCalibrationResponse {

    private UUID templateId;
    private String templateCode;
    private String templateName;
    private MissionType type;
    private MissionPool pool;
    private MissionBand band;
    private UUID categoryId;
    private String categoryName;
    private String tier;
    private Integer week;
    private boolean repeatable;

    private long assigned;
    private long completed;
    private long expired;
    private long stillOpen;
    private Double completionRate;

    private Long players;
    private Long playersCompleted;
    private Long playersExpired;
    private Long playersOpen;
    private Double playerCompletionRate;
    private Double medianCompletionsPerPlayer;

    private Long progressed;
    private Double progressedCompletionRate;

    private Double averageHoursToComplete;
    private Double averageXpReward;
    private long xpPaid;
    private long itemsAwarded;
}
