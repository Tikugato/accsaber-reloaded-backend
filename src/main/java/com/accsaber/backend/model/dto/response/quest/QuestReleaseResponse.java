package com.accsaber.backend.model.dto.response.quest;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QuestReleaseResponse {

    String tag;
    String name;
    String gameVersion;
    Instant publishedAt;
    boolean prerelease;
    boolean latest;
}
