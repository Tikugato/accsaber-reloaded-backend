package com.accsaber.backend.model.dto.response;

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
    Double xParameterValue;
    String yParameterName;
    Double yParameterValue;
    String zParameterName;
    Double zParameterValue;
    Double scale;
    Double shift;
    List<CurvePointResponse> points;
}
