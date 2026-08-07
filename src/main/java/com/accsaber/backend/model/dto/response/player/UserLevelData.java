package com.accsaber.backend.model.dto.response.player;


import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserLevelData {

    int level;
    String title;
    Double xpForCurrentLevel;
    Double xpForNextLevel;
    Double progressPercent;
}
