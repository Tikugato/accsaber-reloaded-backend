package com.accsaber.backend.service.item;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.accsaber.backend.model.event.CrateOpenedEvent;
import com.accsaber.backend.websocket.server.CrateFeedWebSocketHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrateBroadcastService {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final CrateFeedWebSocketHandler crateFeedHandler;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCrateOpened(CrateOpenedEvent event) {
        try {
            crateFeedHandler.broadcast(MAPPER.writeValueAsString(event.payload()));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize crate open for broadcast: {}", e.getMessage());
        }
    }
}
