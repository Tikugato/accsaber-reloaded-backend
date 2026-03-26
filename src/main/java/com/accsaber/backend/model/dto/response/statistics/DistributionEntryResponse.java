package com.accsaber.backend.model.dto.response.statistics;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DistributionEntryResponse {

    private String label;
    private long count;
}
