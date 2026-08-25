package com.accsaber.backend.service.milestone;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.accsaber.backend.model.event.CampaignCompletedEvent;
import com.accsaber.backend.model.event.CampaignNodeCompletedEvent;
import com.accsaber.backend.model.event.CrateOpenedEvent;
import com.accsaber.backend.model.event.MarketListingEvent;
import com.accsaber.backend.model.event.MissionCompletedEvent;
import com.accsaber.backend.service.milestone.source.MilestoneTrigger;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MilestoneTriggerListener {

    private final MilestoneEvaluationService evaluationService;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCrateOpened(CrateOpenedEvent event) {
        if (event.payload() == null || event.payload().player() == null) {
            return;
        }
        dispatch(event.payload().player().getId(), MilestoneTrigger.ITEM);
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMarketListing(MarketListingEvent event) {
        if (event.actor() == null) {
            return;
        }
        dispatch(event.actor().getId(), MilestoneTrigger.MARKET);
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMissionCompleted(MissionCompletedEvent event) {
        if (event.payload() == null) {
            return;
        }
        dispatch(event.payload().getUserId(), MilestoneTrigger.MISSION);
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCampaignCompleted(CampaignCompletedEvent event) {
        if (event.silent()) {
            return;
        }
        dispatch(event.userId(), MilestoneTrigger.CAMPAIGN);
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCampaignNodeCompleted(CampaignNodeCompletedEvent event) {
        if (event.silent()) {
            return;
        }
        dispatch(event.userId(), MilestoneTrigger.CAMPAIGN);
    }

    private void dispatch(Long userId, MilestoneTrigger trigger) {
        if (userId == null) {
            return;
        }
        try {
            evaluationService.evaluateForTrigger(userId, trigger);
        } catch (Exception e) {
            log.error("Milestone {} evaluation failed for user {}: {}", trigger, userId, e.getMessage(), e);
        }
    }
}
