package com.accsaber.backend.model.dto.response.item;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CrateModifierResponse {

    private UserItemResponse.ModifierRef modifier;
    private Double dropChance;
}
