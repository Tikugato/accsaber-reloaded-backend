package com.accsaber.backend.model.dto.response.statistics;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TimeSeriesPointResponse {

    private LocalDate date;
    private long value;
}
