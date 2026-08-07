package com.accsaber.backend.model.dto.response.campaign;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CampaignTextResponse {

    private UUID id;
    private String content;
    private Double positionX;
    private Double positionY;
    private String font;
    private Double scale;
    private String color;
    private String effects;
}
