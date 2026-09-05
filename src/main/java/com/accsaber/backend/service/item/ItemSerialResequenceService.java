package com.accsaber.backend.service.item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.accsaber.backend.exception.ResourceNotFoundException;
import com.accsaber.backend.exception.ValidationException;
import com.accsaber.backend.model.entity.item.Item;
import com.accsaber.backend.model.entity.item.UserItemLink;
import com.accsaber.backend.repository.item.ItemRepository;
import com.accsaber.backend.repository.item.UserItemLinkRepository;
import com.accsaber.backend.util.SqlValues;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemSerialResequenceService {

    private final ItemRepository itemRepository;
    private final UserItemLinkRepository userItemLinkRepository;

    private ItemSerialResequenceService self;

    @Autowired
    @Lazy
    public void setSelf(ItemSerialResequenceService self) {
        this.self = self;
    }

    private record Placement(UUID linkId, Long currentSerial, long targetSerial) {

        boolean moves() {
            return currentSerial == null || currentSerial != targetSerial;
        }
    }

    @Async("taskExecutor")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CompletableFuture<Void> resequenceAll() {
        List<UUID> candidates = itemRepository.findResequenceCandidates();
        log.info("Serial resequence started across {} eligible items", candidates.size());
        int moved = 0;
        int touched = 0;
        for (UUID itemId : candidates) {
            try {
                int itemMoved = self.resequenceItem(itemId);
                if (itemMoved > 0) {
                    touched++;
                    moved += itemMoved;
                }
            } catch (ValidationException e) {
                log.debug("Serial resequence skipped item {}: {}", itemId, e.getMessage());
            } catch (Exception e) {
                log.error("Serial resequence failed for item {}: {}", itemId, e.getMessage(), e);
            }
        }
        log.info("Serial resequence complete - {} serials moved across {} items", moved, touched);
        return CompletableFuture.completedFuture(null);
    }

    @Async("taskExecutor")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CompletableFuture<Void> resequence(UUID itemId) {
        int moved = self.resequenceItem(itemId);
        log.info("Serial resequence complete for item {} - {} serials moved", itemId, moved);
        return CompletableFuture.completedFuture(null);
    }

    @Transactional
    public int resequenceItem(UUID itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item", itemId));
        assertResequenceable(item);

        List<Placement> placements = loadPlacements(itemId);
        if (placements.isEmpty() || placements.stream().noneMatch(Placement::moves)) {
            return 0;
        }

        applySerials(itemId, placements);
        applyFoundersLayer(itemId);
        itemRepository.resetNextSerial(itemId, placements.size() + 1L);

        int moved = (int) placements.stream().filter(Placement::moves).count();
        log.info("Resequenced {} - {} of {} serials moved", item.getName(), moved, placements.size());
        return moved;
    }

    private void assertResequenceable(Item item) {
        if (!item.isSerialized()) {
            throw new ValidationException("itemId", item.getName() + " is not serialized");
        }
        if (item.isTradeable()) {
            throw new ValidationException("itemId",
                    item.getName() + " is tradeable, so its serial numbers cannot be rewritten");
        }
        if (userItemLinkRepository.countUnorderableLinks(item.getId()) > 0) {
            throw new ValidationException("itemId", item.getName()
                    + " has escrowed copies or copies from a source with no achievement time,"
                    + " so its serial order cannot be derived");
        }
    }

    private List<Placement> loadPlacements(UUID itemId) {
        return userItemLinkRepository.findSerialsInEarnedOrder(itemId).stream()
                .map(row -> new Placement(
                        SqlValues.toUuid(row[0]),
                        SqlValues.toLong(row[1]),
                        SqlValues.toLong(row[2])))
                .toList();
    }

    private void applySerials(UUID itemId, List<Placement> placements) {
        userItemLinkRepository.clearSerials(itemId);

        Map<UUID, Long> targets = new HashMap<>();
        for (Placement placement : placements) {
            targets.put(placement.linkId(), placement.targetSerial());
        }

        List<UserItemLink> links = userItemLinkRepository.findAllById(targets.keySet());
        for (UserItemLink link : links) {
            link.setSerialNumber(targets.get(link.getId()));
        }
        userItemLinkRepository.saveAll(links);
        userItemLinkRepository.flush();
    }

    private void applyFoundersLayer(UUID itemId) {
        userItemLinkRepository.stripFoundersAboveSerial(itemId, ModifierResolver.FOUNDERS_THRESHOLD);
        userItemLinkRepository.grantFoundersUpToSerial(itemId, ModifierResolver.FOUNDERS_THRESHOLD);
        userItemLinkRepository.stripRedundantNormal(itemId);
        userItemLinkRepository.grantNormalWhereBare(itemId);
    }
}
