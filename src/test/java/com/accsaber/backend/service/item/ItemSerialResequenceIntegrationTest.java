package com.accsaber.backend.service.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.accsaber.backend.exception.ValidationException;
import com.accsaber.backend.model.entity.item.Item;
import com.accsaber.backend.model.entity.item.ItemModifier;
import com.accsaber.backend.model.entity.item.ItemSource;
import com.accsaber.backend.model.entity.item.ItemType;
import com.accsaber.backend.model.entity.item.UserItemLink;
import com.accsaber.backend.model.entity.milestone.MilestoneSet;
import com.accsaber.backend.model.entity.milestone.UserMilestoneSetBonus;
import com.accsaber.backend.model.entity.user.User;
import com.accsaber.backend.repository.item.UserItemLinkRepository;

import jakarta.persistence.EntityManager;

@Tag("integration")
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ItemSerialResequenceService.class)
class ItemSerialResequenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private UserItemLinkRepository userItemLinkRepository;
    @Autowired
    private ItemSerialResequenceService service;

    private Item item;
    private MilestoneSet set;
    private long nextUserId = 76561190000000501L;

    @BeforeEach
    void seed() {
        ItemType type = ItemType.builder()
                .key("badge-" + UUID.randomUUID())
                .name("Badge")
                .build();
        entityManager.persist(type);

        item = Item.builder()
                .type(type)
                .name("Constellation Badge " + UUID.randomUUID())
                .serialized(true)
                .tradeable(false)
                .build();
        entityManager.persist(item);

        set = MilestoneSet.builder()
                .title("Constellation " + UUID.randomUUID())
                .build();
        entityManager.persist(set);
    }

    private ItemModifier modifier(String key) {
        return entityManager
                .createQuery("SELECT m FROM ItemModifier m WHERE m.key = :key", ItemModifier.class)
                .setParameter("key", key)
                .getSingleResult();
    }

    private UUID holder(Instant earnedAt, long serial, String... modifierKeys) {
        User user = User.builder()
                .id(nextUserId++)
                .name("Holder" + nextUserId)
                .country("ES")
                .build();
        entityManager.persist(user);

        entityManager.persist(UserMilestoneSetBonus.builder()
                .user(user)
                .milestoneSet(set)
                .claimedAt(earnedAt)
                .build());

        Set<ItemModifier> modifiers = new HashSet<>();
        for (String key : modifierKeys) {
            modifiers.add(modifier(key));
        }

        UserItemLink link = UserItemLink.builder()
                .user(user)
                .item(item)
                .modifiers(modifiers)
                .serialNumber(serial)
                .source(ItemSource.milestone_set)
                .sourceId(set.getId().toString())
                .awardedAt(Instant.now())
                .build();
        entityManager.persist(link);
        return link.getId();
    }

    private void flush() {
        entityManager.flush();
        entityManager.clear();
    }

    private Set<String> modifierKeysOf(UUID linkId) {
        List<String> keys = entityManager.createNativeQuery("""
                SELECT m.key FROM user_item_link_modifiers lm
                JOIN item_modifiers m ON m.id = lm.modifier_id
                WHERE lm.user_item_link_id = :linkId
                """, String.class)
                .setParameter("linkId", linkId)
                .getResultList();
        return new HashSet<>(keys);
    }

    private Long serialOf(UUID linkId) {
        return userItemLinkRepository.findById(linkId).orElseThrow().getSerialNumber();
    }

    @Test
    @DisplayName("serials are rewritten to follow the order players earned the item")
    void rewritesSerialsIntoEarnedOrder() {
        UUID first = holder(Instant.parse("2025-01-01T00:00:00Z"), 3L, ItemModifier.NORMAL);
        UUID second = holder(Instant.parse("2025-02-01T00:00:00Z"), 1L, ItemModifier.FOUNDERS);
        UUID third = holder(Instant.parse("2025-03-01T00:00:00Z"), 2L, ItemModifier.FOUNDERS);
        flush();

        int moved = service.resequenceItem(item.getId());
        flush();

        assertThat(moved).isEqualTo(3);
        assertThat(serialOf(first)).isEqualTo(1L);
        assertThat(serialOf(second)).isEqualTo(2L);
        assertThat(serialOf(third)).isEqualTo(3L);
    }

    @Test
    @DisplayName("Founder's follows the new lowest five, and everyone above it falls back to normal")
    void movesFoundersWithTheNewSerials() {
        UUID newestFirst = holder(Instant.parse("2025-01-01T00:00:00Z"), 6L, ItemModifier.NORMAL);
        List<UUID> displaced = List.of(
                holder(Instant.parse("2025-02-01T00:00:00Z"), 1L, ItemModifier.FOUNDERS),
                holder(Instant.parse("2025-02-02T00:00:00Z"), 2L, ItemModifier.FOUNDERS),
                holder(Instant.parse("2025-02-03T00:00:00Z"), 3L, ItemModifier.FOUNDERS),
                holder(Instant.parse("2025-02-04T00:00:00Z"), 4L, ItemModifier.FOUNDERS),
                holder(Instant.parse("2025-02-05T00:00:00Z"), 5L, ItemModifier.FOUNDERS));
        flush();

        service.resequenceItem(item.getId());
        flush();

        assertThat(serialOf(newestFirst)).isEqualTo(1L);
        assertThat(modifierKeysOf(newestFirst)).containsExactly(ItemModifier.FOUNDERS);

        assertThat(serialOf(displaced.get(4))).isEqualTo(6L);
        assertThat(modifierKeysOf(displaced.get(4))).containsExactly(ItemModifier.NORMAL);

        assertThat(modifierKeysOf(displaced.get(0))).containsExactly(ItemModifier.FOUNDERS);
    }

    @Test
    @DisplayName("an unrelated per-instance modifier survives the renumber")
    void preservesOtherPerInstanceModifiers() {
        UUID strangeHolder = holder(Instant.parse("2025-01-01T00:00:00Z"), 9L,
                ItemModifier.NORMAL, ItemModifier.STRANGE);
        flush();

        service.resequenceItem(item.getId());
        flush();

        assertThat(serialOf(strangeHolder)).isEqualTo(1L);
        assertThat(modifierKeysOf(strangeHolder))
                .containsExactlyInAnyOrder(ItemModifier.FOUNDERS, ItemModifier.STRANGE);
    }

    @Test
    @DisplayName("the item's next serial continues after the last renumbered copy")
    void resetsNextSerial() {
        holder(Instant.parse("2025-01-01T00:00:00Z"), 40L, ItemModifier.NORMAL);
        holder(Instant.parse("2025-02-01T00:00:00Z"), 41L, ItemModifier.NORMAL);
        flush();

        service.resequenceItem(item.getId());
        flush();

        Long nextSerial = (Long) entityManager
                .createNativeQuery("SELECT next_serial FROM items WHERE id = :id", Long.class)
                .setParameter("id", item.getId())
                .getSingleResult();
        assertThat(nextSerial).isEqualTo(3L);
    }

    @Test
    @DisplayName("an escrowed copy blocks the renumber rather than reordering around it")
    void refusesWhenACopyIsEscrowed() {
        UUID linkId = holder(Instant.parse("2025-01-01T00:00:00Z"), 1L, ItemModifier.NORMAL);
        userItemLinkRepository.findById(linkId).orElseThrow().setEscrowed(true);
        flush();

        assertThatThrownBy(() -> service.resequenceItem(item.getId()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("an item already in earned order is left completely alone")
    void leavesCorrectlyOrderedItemUntouched() {
        holder(Instant.parse("2025-01-01T00:00:00Z"), 1L, ItemModifier.FOUNDERS);
        holder(Instant.parse("2025-02-01T00:00:00Z"), 2L, ItemModifier.FOUNDERS);
        flush();

        assertThat(service.resequenceItem(item.getId())).isZero();
    }
}
