package com.accsaber.backend.model.dto.response.map;


import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AiComplexityResponse {
    Double complexity;
}
