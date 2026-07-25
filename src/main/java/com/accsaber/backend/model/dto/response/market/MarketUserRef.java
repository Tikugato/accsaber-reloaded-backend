package com.accsaber.backend.model.dto.response.market;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MarketUserRef {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String name;
    private String avatarUrl;
    private String cdnAvatarUrl;
    private String country;
}
