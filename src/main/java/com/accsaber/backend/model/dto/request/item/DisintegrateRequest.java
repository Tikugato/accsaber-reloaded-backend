package com.accsaber.backend.model.dto.request.item;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DisintegrateRequest {

    @NotEmpty
    @Size(max = 200)
    @Valid
    private List<Entry> entries;

    @Data
    public static class Entry {

        @NotNull
        private UUID linkId;

        @Positive
        private Long quantity;
    }
}
