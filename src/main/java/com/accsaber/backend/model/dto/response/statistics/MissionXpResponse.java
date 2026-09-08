package com.accsaber.backend.model.dto.response.statistics;

import java.util.UUID;

import com.accsaber.backend.model.entity.mission.MissionBand;
import com.accsaber.backend.model.entity.mission.MissionPool;
import com.accsaber.backend.model.entity.mission.MissionType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MissionXpResponse {

    private UUID templateId;
    private String templateCode;
    private String templateName;
    private MissionType type;
    private MissionPool pool;
    private MissionBand band;
    private long completed;
    private long xpPaid;
    private Double averageXp;
    private Double medianXp;
    private Double p90Xp;
    private Double shareOfMissionXp;
    private long itemsAwarded;
}
