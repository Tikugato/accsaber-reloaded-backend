package com.accsaber.backend.model.dto.request.staff;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForceChangePasswordRequest {

    @NotBlank
    private String newPassword;
}
