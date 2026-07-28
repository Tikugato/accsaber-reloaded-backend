package com.accsaber.backend.service.campaign;

public record CampaignEditor(Long userId, boolean privileged) {

    public static CampaignEditor player(Long userId) {
        return new CampaignEditor(userId, false);
    }

    public static CampaignEditor staff(Long linkedUserId) {
        return new CampaignEditor(linkedUserId, true);
    }
}
