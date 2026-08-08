package com.accsaber.backend.model.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PairingRedeemRequest {

    @NotBlank
    private String code;
}
