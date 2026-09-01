package com.accsaber.backend.model.dto.response.item;

import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DisintegrationResponse {

    private List<Entry> entries;
    private long essenceGained;
    private long balance;

    @Getter
    @Builder
    public static class Entry {

        private UUID linkId;
        private UUID itemId;
        private long quantityDisintegrated;
        private Long remainingQuantity;
        private long essenceGained;
    }
}
