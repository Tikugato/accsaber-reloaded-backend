package com.accsaber.backend.model.dto.request.curve;

import java.math.BigDecimal;

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
    private BigDecimal xParameterValue;

    private String yParameterName;
    private BigDecimal yParameterValue;

    private String zParameterName;
    private BigDecimal zParameterValue;

    private BigDecimal scale;
    private BigDecimal shift;
}
