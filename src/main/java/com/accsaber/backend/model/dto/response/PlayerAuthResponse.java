package com.accsaber.backend.model.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PlayerAuthResponse {

    String accessToken;
    String refreshToken;
    long expiresIn;
    @JsonSerialize(using = ToStringSerializer.class)
    Long userId;
}
