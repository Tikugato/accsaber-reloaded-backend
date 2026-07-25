package com.accsaber.backend.model.event;

import com.accsaber.backend.websocket.server.CrateOpenBroadcast;

public record CrateOpenedEvent(CrateOpenBroadcast payload) {
}
