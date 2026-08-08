package com.accsaber.backend.service.mission;


import com.accsaber.backend.model.entity.map.MapDifficulty;

record MapPick(MapDifficulty difficulty, double complexity, Integer maxScore) {
}
