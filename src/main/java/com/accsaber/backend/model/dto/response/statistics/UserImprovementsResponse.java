package com.accsaber.backend.model.dto.response.statistics;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserImprovementsResponse {

    private String userId;
    private String userName;
    private String avatarUrl;
    private String country;
    private long improvementCount;
}
