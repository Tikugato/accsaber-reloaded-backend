package com.accsaber.backend.model.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CurveResponse {

    UUID id;
    String name;
    String type;
    String formula;
    String xParameterName;
    BigDecimal xParameterValue;
    String yParameterName;
    BigDecimal yParameterValue;
    String zParameterName;
    BigDecimal zParameterValue;
    BigDecimal scale;
    BigDecimal shift;
    List<CurvePointResponse> points;
}
