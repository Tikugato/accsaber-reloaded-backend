package com.accsaber.backend.service.milestone.source;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class MilestoneSourceRegistry {

    private final Map<String, MilestoneSource> sources = new LinkedHashMap<>();

    public MilestoneSourceRegistry(List<MilestoneSourceProvider> providers) {
        for (MilestoneSourceProvider provider : providers) {
            for (MilestoneSource source : provider.sources()) {
                MilestoneSource clash = sources.putIfAbsent(source.name(), source);
                if (clash != null) {
                    throw new IllegalStateException("Duplicate milestone source: " + source.name());
                }
            }
        }
    }

    public MilestoneSource get(String name) {
        return sources.get(name);
    }

    public Collection<String> names() {
        return sources.keySet();
    }

    public List<String> namesFor(MilestoneTrigger trigger) {
        return sources.values().stream()
                .filter(source -> source.triggers().contains(trigger))
                .map(MilestoneSource::name)
                .toList();
    }

    public Collection<MilestoneSource> all() {
        return sources.values();
    }
}
