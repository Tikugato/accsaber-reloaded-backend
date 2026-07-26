package com.accsaber.backend.model.entity.campaign;

import jakarta.persistence.Column;
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
public class CampaignBackgroundPlacement {

    @Column(name = "background_size")
    private Integer size;

    @Column(name = "background_x")
    private Integer x;

    @Column(name = "background_y")
    private Integer y;
}
