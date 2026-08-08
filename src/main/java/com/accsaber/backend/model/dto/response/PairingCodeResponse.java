package com.accsaber.backend.model.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PairingCodeResponse {

    String code;
    long expiresIn;
}
