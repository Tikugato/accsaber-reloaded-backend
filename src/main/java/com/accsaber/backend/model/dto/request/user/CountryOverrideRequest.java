package com.accsaber.backend.model.dto.request.user;

import jakarta.validation.constraints.Pattern;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
public class CountryOverrideRequest {

    @Schema(description = "Two-letter ISO 3166-1 alpha-2 code to pin the player to, or null to lift the override.", nullable = true)
    @Pattern(regexp = "^[A-Z]{2}$", message = "Country must be a 2-letter ISO 3166-1 alpha-2 code (e.g. DE, US)")
    private String country;
}
