package com.accsaber.backend.websocket.server;

import com.accsaber.backend.model.dto.response.item.CrateOpenResponse;
import com.accsaber.backend.model.dto.response.market.MarketUserRef;

public record CrateOpenBroadcast(String type, MarketUserRef player, CrateOpenResponse open) {
}
