package com.accsaber.backend.model.dto.response;

import java.util.List;

import com.accsaber.backend.model.entity.staff.StaffRole;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PlayerAuthResponse {

    String accessToken;
    String refreshToken;
    long expiresIn;
    String userId;
    List<StaffRole> roles;
}
