package com.accsaber.backend.model.dto.response.badge;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BadgeResponse {

    UUID id;
    String name;
    String description;
    String imageUrl;
    Instant createdAt;
}
