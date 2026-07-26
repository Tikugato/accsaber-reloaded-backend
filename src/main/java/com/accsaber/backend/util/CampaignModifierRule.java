package com.accsaber.backend.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.accsaber.backend.model.entity.campaign.CampaignDifficultyModifier;
import com.accsaber.backend.model.entity.campaign.CampaignModifierRequirement;

public record CampaignModifierRule(Set<UUID> required, Set<UUID> forbidden) {

    public boolean isEmpty() {
        return required.isEmpty() && forbidden.isEmpty();
    }

    public boolean matches(Set<UUID> scoreModifierIds) {
        if (!scoreModifierIds.containsAll(required)) {
            return false;
        }
        for (UUID id : forbidden) {
            if (scoreModifierIds.contains(id)) {
                return false;
            }
        }
        return true;
    }

    public static Map<UUID, CampaignModifierRule> byNode(Collection<CampaignDifficultyModifier> links) {
        if (links.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Set<UUID>> required = new HashMap<>();
        Map<UUID, Set<UUID>> forbidden = new HashMap<>();
        for (CampaignDifficultyModifier link : links) {
            UUID nodeId = link.getId().getCampaignDifficultyId();
            UUID modifierId = link.getId().getModifierId();
            Map<UUID, Set<UUID>> target = link.getRequirement() == CampaignModifierRequirement.FORBIDDEN
                    ? forbidden
                    : required;
            target.computeIfAbsent(nodeId, k -> new HashSet<>()).add(modifierId);
        }
        Map<UUID, CampaignModifierRule> rules = new HashMap<>();
        Set<UUID> nodeIds = new HashSet<>(required.keySet());
        nodeIds.addAll(forbidden.keySet());
        for (UUID nodeId : nodeIds) {
            rules.put(nodeId, new CampaignModifierRule(
                    required.getOrDefault(nodeId, Set.of()),
                    forbidden.getOrDefault(nodeId, Set.of())));
        }
        return rules;
    }
}
