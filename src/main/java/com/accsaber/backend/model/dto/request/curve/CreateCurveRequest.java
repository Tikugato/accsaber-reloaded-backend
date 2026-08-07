package com.accsaber.backend.model.dto.request.curve;


import com.accsaber.backend.model.entity.CurveType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCurveRequest {

    @NotBlank
    private String name;

    @NotNull
    private CurveType type;

    private String formula;

    private String xParameterName;
    private Double xParameterValue;

    private String yParameterName;
    private Double yParameterValue;

    private String zParameterName;
    private Double zParameterValue;

    private Double scale;
    private Double shift;
}
