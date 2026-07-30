package com.accsaber.backend.service.infra;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.accsaber.backend.model.entity.Modifier;
import com.accsaber.backend.repository.ModifierRepository;
import com.accsaber.backend.util.PlatformScoreMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ModifierCacheService {

    private final ModifierRepository modifierRepository;
    private volatile Map<String, UUID> modifierCodeToId;
    private volatile Set<UUID> bannedModifierIds;

    public Map<String, UUID> getModifierCodeToId() {
        if (modifierCodeToId == null) {
            synchronized (this) {
                if (modifierCodeToId == null) {
                    modifierCodeToId = modifierRepository.findByActiveTrue().stream()
                            .collect(Collectors.toMap(Modifier::getCode, Modifier::getId));
                }
            }
        }
        return modifierCodeToId;
    }

    public Set<UUID> getBannedModifierIds() {
        if (bannedModifierIds == null) {
            synchronized (this) {
                if (bannedModifierIds == null) {
                    bannedModifierIds = getModifierCodeToId().entrySet().stream()
                            .filter(e -> PlatformScoreMapper.BANNED_MODIFIER_CODES.contains(e.getKey()))
                            .map(Map.Entry::getValue)
                            .collect(Collectors.toUnmodifiableSet());
                }
            }
        }
        return bannedModifierIds;
    }

    public boolean containsBannedModifier(Collection<UUID> modifierIds) {
        if (modifierIds == null || modifierIds.isEmpty()) {
            return false;
        }
        Set<UUID> banned = getBannedModifierIds();
        return modifierIds.stream().anyMatch(banned::contains);
    }

    public void invalidate() {
        modifierCodeToId = null;
        bannedModifierIds = null;
    }
}
