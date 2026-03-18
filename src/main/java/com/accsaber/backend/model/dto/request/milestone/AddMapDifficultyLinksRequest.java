package com.accsaber.backend.model.dto.request.milestone;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class AddMapDifficultyLinksRequest {

    @NotEmpty
    private List<UUID> mapDifficultyIds;
}
