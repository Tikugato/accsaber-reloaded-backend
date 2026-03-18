package com.accsaber.backend.model.dto.request.badge;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateBadgeRequest {

    @NotBlank
    private String name;

    private String description;

    private String imageUrl;
}
