package com.accsaber.backend.model.dto.request.item;


import lombok.Data;

@Data
public class UpdateItemModifierRequest {

    private Double globalDropChance;
    private String seasonStart;
    private String seasonEnd;
}
