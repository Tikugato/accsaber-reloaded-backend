package com.accsaber.backend.model.dto.response;

import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthMeResponse {

    @JsonSerialize(using = ToStringSerializer.class)
    Long userId;
    String name;
    String avatarUrl;
    String cdnAvatarUrl;
    String country;
    boolean banned;
    List<OauthConnectionSummary> connections;
    StaffContext staff;

    @Value
    @Builder
    public static class OauthConnectionSummary {
        String provider;
        String providerUserId;
        String providerUsername;
        String providerAvatarUrl;
    }

    @Value
    @Builder
    public static class StaffContext {
        String staffId;
        String role;
        String status;
    }
}
