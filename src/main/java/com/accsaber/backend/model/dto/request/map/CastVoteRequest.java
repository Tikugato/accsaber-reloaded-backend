package com.accsaber.backend.model.dto.request.map;


import com.accsaber.backend.model.entity.map.MapVoteAction;
import com.accsaber.backend.model.entity.map.VoteType;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CastVoteRequest {

    @NotNull
    private VoteType vote;

    @NotNull
    private MapVoteAction type;

    private Double suggestedComplexity;

    private VoteType criteriaVote;

    private Boolean criteriaVoteOverride;

    private String reason;
}
