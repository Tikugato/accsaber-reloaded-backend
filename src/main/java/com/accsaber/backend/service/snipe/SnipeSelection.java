package com.accsaber.backend.service.snipe;

import java.util.List;

import com.accsaber.backend.model.entity.map.MapDifficulty;
import com.accsaber.backend.model.entity.user.User;

public record SnipeSelection(User target, String categoryLabel, List<MapDifficulty> difficulties) {
}
