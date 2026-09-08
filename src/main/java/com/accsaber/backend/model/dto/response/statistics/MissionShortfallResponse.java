package com.accsaber.backend.model.dto.response.statistics;

import java.util.List;

import com.accsaber.backend.model.entity.mission.MissionBand;
import com.accsaber.backend.model.entity.mission.MissionProgressAxis;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MissionShortfallResponse {

    private MissionBand band;
    private MissionProgressAxis axis;
    private long failed;
    private long measured;
    private Double medianReachedFraction;
    private List<DistributionEntryResponse> buckets;
}
