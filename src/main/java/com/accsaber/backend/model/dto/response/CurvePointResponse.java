package com.accsaber.backend.model.dto.response;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CurvePointResponse {

    BigDecimal x;
    BigDecimal y;
}
