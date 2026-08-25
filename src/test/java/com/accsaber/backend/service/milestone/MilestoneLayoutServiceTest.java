package com.accsaber.backend.service.milestone;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.accsaber.backend.model.entity.milestone.Milestone;
import com.accsaber.backend.model.entity.milestone.MilestonePrerequisiteLink;
import com.accsaber.backend.model.entity.milestone.MilestoneSet;
import com.accsaber.backend.model.entity.milestone.MilestoneTier;

class MilestoneLayoutServiceTest {

    private static final double MIN_SEPARATION = 20.0;

    private final MilestoneLayoutService service = new MilestoneLayoutService(null, null);

    private final MilestoneSet set = MilestoneSet.builder().id(UUID.randomUUID()).title("Set").build();

    private Milestone milestone(String title, MilestoneTier tier) {
        return Milestone.builder()
                .id(UUID.randomUUID())
                .milestoneSet(set)
                .title(title)
                .type("milestone")
                .tier(tier)
                .active(true)
                .build();
    }

    private MilestonePrerequisiteLink link(Milestone child, Milestone parent) {
        return MilestonePrerequisiteLink.builder()
                .id(UUID.randomUUID())
                .milestone(child)
                .prerequisiteMilestone(parent)
                .active(true)
                .build();
    }

    private double closestPair(Map<UUID, MilestoneLayoutService.Point> positions) {
        List<MilestoneLayoutService.Point> points = new ArrayList<>(positions.values());
        double closest = Double.MAX_VALUE;
        for (int i = 0; i < points.size(); i++) {
            for (int j = i + 1; j < points.size(); j++) {
                closest = Math.min(closest, Math.hypot(
                        points.get(i).x() - points.get(j).x(),
                        points.get(i).y() - points.get(j).y()));
            }
        }
        return closest;
    }

    @Nested
    @DisplayName("chain layout")
    class ChainLayout {

        @Test
        void placesEachDepthFurtherRight() {
            Milestone root = milestone("Root", MilestoneTier.bronze);
            Milestone mid = milestone("Mid", MilestoneTier.silver);
            Milestone leaf = milestone("Leaf", MilestoneTier.gold);

            Map<UUID, MilestoneLayoutService.Point> positions = service.layout(
                    List.of(root, mid, leaf),
                    List.of(link(mid, root), link(leaf, mid)));

            assertThat(positions).hasSize(3);
            assertThat(positions.get(mid.getId()).x()).isGreaterThan(positions.get(root.getId()).x());
            assertThat(positions.get(leaf.getId()).x()).isGreaterThan(positions.get(mid.getId()).x());
        }

        @Test
        void isDeterministicForTheSameGraph() {
            Milestone root = milestone("Root", MilestoneTier.bronze);
            Milestone leaf = milestone("Leaf", MilestoneTier.silver);
            List<Milestone> milestones = List.of(root, leaf);
            List<MilestonePrerequisiteLink> links = List.of(link(leaf, root));

            assertThat(service.layout(milestones, links))
                    .isEqualTo(service.layout(milestones, links));
        }
    }

    @Nested
    @DisplayName("separation")
    class Separation {

        @Test
        void keepsWideSiblingFansApart() {
            Milestone root = milestone("Root", MilestoneTier.bronze);
            List<Milestone> all = new ArrayList<>(List.of(root));
            List<MilestonePrerequisiteLink> links = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                Milestone child = milestone("Child " + i, MilestoneTier.silver);
                all.add(child);
                links.add(link(child, root));
            }

            assertThat(closestPair(service.layout(all, links)))
                    .isGreaterThanOrEqualTo(MIN_SEPARATION - 0.01);
        }

        @Test
        void spreadsDisconnectedMilestonesOnACircle() {
            List<Milestone> all = new ArrayList<>();
            for (int i = 0; i < 14; i++) {
                all.add(milestone("Loose " + i, MilestoneTier.bronze));
            }

            Map<UUID, MilestoneLayoutService.Point> positions = service.layout(all, List.of());

            assertThat(positions).hasSize(14);
            assertThat(closestPair(positions)).isGreaterThan(5.0);
        }
    }

    @Nested
    @DisplayName("origin")
    class Origin {

        @Test
        void normalisesEverySetToTheSameTopLeftPadding() {
            Milestone root = milestone("Root", MilestoneTier.bronze);
            Milestone leaf = milestone("Leaf", MilestoneTier.silver);

            Map<UUID, MilestoneLayoutService.Point> positions = service.layout(
                    List.of(root, leaf), List.of(link(leaf, root)));

            double minX = positions.values().stream().mapToDouble(MilestoneLayoutService.Point::x).min().orElseThrow();
            double minY = positions.values().stream().mapToDouble(MilestoneLayoutService.Point::y).min().orElseThrow();
            assertThat(minX).isEqualTo(10.0);
            assertThat(minY).isEqualTo(10.0);
        }

        @Test
        void neverEmitsNegativeCoordinates() {
            Milestone root = milestone("Root", MilestoneTier.bronze);
            List<Milestone> all = new ArrayList<>(List.of(root));
            List<MilestonePrerequisiteLink> links = new ArrayList<>();
            for (int i = 0; i < 9; i++) {
                Milestone child = milestone("Child " + i, MilestoneTier.gold);
                all.add(child);
                links.add(link(child, root));
            }

            assertThat(service.layout(all, links).values())
                    .allSatisfy(p -> {
                        assertThat(p.x()).isGreaterThanOrEqualTo(0.0);
                        assertThat(p.y()).isGreaterThanOrEqualTo(0.0);
                    });
        }
    }

    @Nested
    @DisplayName("cross-set prerequisites")
    class CrossSet {

        @Test
        void ignoresParentsOutsideTheSetButStillPlacesTheChild() {
            Milestone outside = milestone("Outside", MilestoneTier.bronze);
            Milestone inside = milestone("Inside", MilestoneTier.silver);

            Map<UUID, MilestoneLayoutService.Point> positions = service.layout(
                    List.of(inside), List.of(link(inside, outside)));

            assertThat(positions).containsOnlyKeys(inside.getId());
        }
    }
}
