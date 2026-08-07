package com.accsaber.backend.model.dto.request.campaign;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MoveCampaignElementsRequest {

    @NotEmpty
    @Size(max = 400)
    @Valid
    private List<Move> moves;

    @Data
    public static class Move {

        @NotNull
        private UUID id;

        @NotNull
        private Double positionX;

        @NotNull
        private Double positionY;
    }
}
