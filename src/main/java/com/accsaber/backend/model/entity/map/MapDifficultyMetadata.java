package com.accsaber.backend.model.entity.map;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MapDifficultyMetadata {

    private Double bpm;
    private Integer notes;
    private Integer bombs;
    private Integer walls;
    private Integer duration;
}
