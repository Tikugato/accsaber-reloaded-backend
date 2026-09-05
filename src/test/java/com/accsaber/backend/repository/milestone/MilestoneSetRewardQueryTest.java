package com.accsaber.backend.repository.milestone;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.accsaber.backend.model.entity.item.Item;
import com.accsaber.backend.model.entity.item.ItemSource;
import com.accsaber.backend.model.entity.item.ItemType;
import com.accsaber.backend.model.entity.item.UserItemLink;
import com.accsaber.backend.model.entity.milestone.Milestone;
import com.accsaber.backend.model.entity.milestone.MilestoneSet;
import com.accsaber.backend.model.entity.milestone.MilestoneSetItem;
import com.accsaber.backend.model.entity.milestone.MilestoneStatus;
import com.accsaber.backend.model.entity.milestone.UserMilestoneLink;
import com.accsaber.backend.model.entity.milestone.UserMilestoneSetBonus;
import com.accsaber.backend.model.entity.user.User;
import com.accsaber.backend.util.SqlValues;

import jakarta.persistence.EntityManager;

@Tag("integration")
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MilestoneSetRewardQueryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private UserMilestoneSetBonusRepository repository;

    private User player;
    private MilestoneSet set;
    private Milestone first;
    private Milestone second;

    @BeforeEach
    void seed() {
        player = User.builder()
                .id(76561190000000101L)
                .name("SetPlayer")
                .country("ES")
                .build();
        entityManager.persist(player);

        set = MilestoneSet.builder()
                .title("Constellation " + UUID.randomUUID())
                .setBonusXp(500.0)
                .build();
        entityManager.persist(set);

        first = persistMilestone("First");
        second = persistMilestone("Second");
    }

    private Milestone persistMilestone(String title) {
        Milestone milestone = Milestone.builder()
                .milestoneSet(set)
                .title(title)
                .type("milestone")
                .status(MilestoneStatus.ACTIVE)
                .xp(100.0)
                .targetValue(1.0)
                .active(true)
                .build();
        entityManager.persist(milestone);
        return milestone;
    }

    private void complete(Milestone milestone, Instant at) {
        entityManager.persist(UserMilestoneLink.builder()
                .user(player)
                .milestone(milestone)
                .completed(true)
                .completedAt(at)
                .build());
    }

    private Item persistItem(String name) {
        ItemType type = ItemType.builder()
                .key("badge-" + UUID.randomUUID())
                .name("Badge")
                .build();
        entityManager.persist(type);

        Item item = Item.builder()
                .type(type)
                .name(name)
                .build();
        entityManager.persist(item);
        return item;
    }

    private void attachReward(Item item) {
        entityManager.persist(MilestoneSetItem.builder()
                .id(new MilestoneSetItem.MilestoneSetItemId(set.getId(), item.getId()))
                .milestoneSet(set)
                .item(item)
                .quantity(1)
                .build());
    }

    private UserMilestoneSetBonus claimBonus(Instant at) {
        UserMilestoneSetBonus bonus = UserMilestoneSetBonus.builder()
                .user(player)
                .milestoneSet(set)
                .claimedAt(at)
                .build();
        entityManager.persist(bonus);
        return bonus;
    }

    private void flush() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("a fully completed set with no bonus row is reported, stamped with the last completion")
    void reportsUnclaimedSetCompletion() {
        Instant last = Instant.parse("2025-03-02T12:00:00Z");
        complete(first, Instant.parse("2025-01-01T12:00:00Z"));
        complete(second, last);
        flush();

        List<Object[]> rows = repository.findUnclaimedSetCompletions(100);

        assertThat(rows).hasSize(1);
        assertThat(SqlValues.toLong(rows.get(0)[0])).isEqualTo(player.getId());
        assertThat(SqlValues.toUuid(rows.get(0)[1])).isEqualTo(set.getId());
        assertThat(SqlValues.toInstant(rows.get(0)[2])).isEqualTo(last);
    }

    @Test
    @DisplayName("a partially completed set is not reported")
    void ignoresPartiallyCompletedSet() {
        complete(first, Instant.parse("2025-01-01T12:00:00Z"));
        flush();

        assertThat(repository.findUnclaimedSetCompletions(100)).isEmpty();
    }

    @Test
    @DisplayName("a set that already has its bonus row is not reported")
    void ignoresAlreadyClaimedSet() {
        complete(first, Instant.parse("2025-01-01T12:00:00Z"));
        complete(second, Instant.parse("2025-03-02T12:00:00Z"));
        claimBonus(Instant.parse("2025-03-02T12:00:00Z"));
        flush();

        assertThat(repository.findUnclaimedSetCompletions(100)).isEmpty();
    }

    @Test
    @DisplayName("an inactive player is not reported")
    void ignoresInactivePlayer() {
        player.setActive(false);
        complete(first, Instant.parse("2025-01-01T12:00:00Z"));
        complete(second, Instant.parse("2025-03-02T12:00:00Z"));
        flush();

        assertThat(repository.findUnclaimedSetCompletions(100)).isEmpty();
    }

    @Test
    @DisplayName("a claimed bonus whose reward item was never handed out is reported")
    void reportsBonusMissingItsReward() {
        Instant claimedAt = Instant.parse("2025-03-02T12:00:00Z");
        attachReward(persistItem("Novice No More"));
        claimBonus(claimedAt);
        flush();

        List<Object[]> rows = repository.findBonusesMissingRewards(100);

        assertThat(rows).hasSize(1);
        assertThat(SqlValues.toLong(rows.get(0)[0])).isEqualTo(player.getId());
        assertThat(SqlValues.toUuid(rows.get(0)[1])).isEqualTo(set.getId());
        assertThat(SqlValues.toInstant(rows.get(0)[2])).isEqualTo(claimedAt);
    }

    @Test
    @DisplayName("a claimed bonus whose reward the player already holds is not reported")
    void ignoresBonusWithGrantedReward() {
        Item item = persistItem("Box Ticker");
        attachReward(item);
        claimBonus(Instant.parse("2025-03-02T12:00:00Z"));
        entityManager.persist(UserItemLink.builder()
                .user(player)
                .item(item)
                .source(ItemSource.milestone_set)
                .sourceId(set.getId().toString())
                .build());
        flush();

        assertThat(repository.findBonusesMissingRewards(100)).isEmpty();
    }

    @Test
    @DisplayName("a set with no reward items attached is not reported")
    void ignoresBonusWithoutRewards() {
        claimBonus(Instant.parse("2025-03-02T12:00:00Z"));
        flush();

        assertThat(repository.findBonusesMissingRewards(100)).isEmpty();
    }
}
