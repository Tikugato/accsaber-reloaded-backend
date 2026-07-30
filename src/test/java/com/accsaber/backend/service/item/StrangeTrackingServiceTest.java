package com.accsaber.backend.service.item;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.accsaber.backend.model.dto.response.score.ScoreResponse;
import com.accsaber.backend.model.entity.item.ItemModifier;
import com.accsaber.backend.model.entity.item.UserItemLink;
import com.accsaber.backend.model.event.ScoreSubmittedEvent;
import com.accsaber.backend.repository.item.UserItemLinkCounterRepository;

@ExtendWith(MockitoExtension.class)
class StrangeTrackingServiceTest {

    private static final Long USER_ID = 76561198087536397L;

    @Mock
    private ItemService itemService;
    @Mock
    private UserItemLinkCounterRepository counterRepository;

    @InjectMocks
    private StrangeTrackingService strangeTrackingService;

    @Test
    void activeScoreIncrementsEveryEquippedStrangeLink() {
        UserItemLink strange = link("strange", "founders");
        UserItemLink normal = link("normal");
        when(itemService.findEffectiveEquippedLinks(USER_ID)).thenReturn(List.of(strange, normal));

        strangeTrackingService.onScoreSubmitted(event(true));

        verify(counterRepository).incrementBy(strange.getId(), StrangeTrackingService.STAT_PLAY_COUNT, 1L);
        verify(counterRepository, never()).incrementBy(eq(normal.getId()), eq(StrangeTrackingService.STAT_PLAY_COUNT),
                eq(1L));
    }

    @Test
    void inactiveScoreDoesNotCount() {
        strangeTrackingService.onScoreSubmitted(event(false));

        verifyNoInteractions(itemService, counterRepository);
    }

    @Test
    void unparseableUserIdIsIgnored() {
        strangeTrackingService.onScoreSubmitted(new ScoreSubmittedEvent(
                ScoreResponse.builder().userId("not-a-number").active(true).build()));

        verifyNoInteractions(itemService, counterRepository);
    }

    private static ScoreSubmittedEvent event(boolean active) {
        return new ScoreSubmittedEvent(ScoreResponse.builder()
                .userId(String.valueOf(USER_ID))
                .active(active)
                .build());
    }

    private static UserItemLink link(String... modifierKeys) {
        Set<ItemModifier> modifiers = new java.util.HashSet<>();
        for (String key : modifierKeys) {
            modifiers.add(ItemModifier.builder().id(UUID.randomUUID()).key(key).build());
        }
        return UserItemLink.builder()
                .id(UUID.randomUUID())
                .modifiers(modifiers)
                .build();
    }
}
