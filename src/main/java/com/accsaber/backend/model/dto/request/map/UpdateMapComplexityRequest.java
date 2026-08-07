package com.accsaber.backend.model.dto.request.map;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class UpdateMapComplexityRequest {

    @NotNull
    @Positive
    private Double complexity;

    private String reason;
}
