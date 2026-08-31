package com.accsaber.backend.model.dto.request.user;

import java.util.UUID;

public record PinnedMilestoneEntry(UUID milestoneId, int displayOrder) {
}
