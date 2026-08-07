package com.accsaber.backend.model.dto.request.curve;


import lombok.Data;

@Data
public class UpdateCurveRequest {

    private String name;
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
