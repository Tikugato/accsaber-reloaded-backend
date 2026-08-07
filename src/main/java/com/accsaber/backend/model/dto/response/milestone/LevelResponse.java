package com.accsaber.backend.model.dto.response.milestone;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LevelResponse {

    private int level;
    private String title;
    private Double totalXp;
    private Double xpForCurrentLevel;
    private Double xpForNextLevel;
    private Double progressPercent;
}
