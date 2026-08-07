package com.accsaber.backend.model.dto.response;


import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CurvePointResponse {

    Double x;
    Double y;
}
