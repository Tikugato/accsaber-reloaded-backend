package com.accsaber.backend.model.dto.projection;

import java.util.UUID;

public record ScoreModifierRow(UUID scoreId, UUID modifierId, String code) {
}
