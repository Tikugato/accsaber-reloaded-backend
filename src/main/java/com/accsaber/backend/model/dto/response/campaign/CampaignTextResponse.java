package com.accsaber.backend.model.dto.response.campaign;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CampaignTextResponse {

    private UUID id;
    private String content;
    private BigDecimal positionX;
    private BigDecimal positionY;
    private String font;
    private BigDecimal scale;
    private String color;
    private String effects;
}
