package com.accsaber.backend.model.dto.response.milestone;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MilestoneSetLinkResponse {

    private UUID id;
    private UUID groupId;
    private String groupName;
    private UUID setId;
    private String setTitle;
    private int sortOrder;
    private Instant createdAt;
}
