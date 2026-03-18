package com.accsaber.backend.model.dto.request.curve;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class UpdateCurveRequest {

    private String name;
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
