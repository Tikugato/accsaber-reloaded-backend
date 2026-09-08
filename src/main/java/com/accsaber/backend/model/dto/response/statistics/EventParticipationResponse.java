package com.accsaber.backend.model.dto.response.statistics;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventParticipationResponse {

    private UUID eventId;
    private String title;
    private String slug;
    private String iconUrl;
    private Instant startsAt;
    private Instant endsAt;
    private long participants;
    private long finishers;
    private Double finishRate;
    private long missionsCompleted;
    private Double averageMissionsPerParticipant;
}
