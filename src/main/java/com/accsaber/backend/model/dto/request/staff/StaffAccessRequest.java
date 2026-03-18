package com.accsaber.backend.model.dto.request.staff;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StaffAccessRequest {

    private String username;

    private String email;

    @NotBlank
    private String password;
}
