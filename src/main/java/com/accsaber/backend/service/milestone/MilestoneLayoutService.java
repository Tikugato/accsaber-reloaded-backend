package com.accsaber.backend.service.milestone;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.accsaber.backend.model.entity.milestone.Milestone;
import com.accsaber.backend.model.entity.milestone.MilestonePrerequisiteLink;
import com.accsaber.backend.model.entity.milestone.MilestoneTier;
import com.accsaber.backend.repository.milestone.MilestonePrerequisiteLinkRepository;
import com.accsaber.backend.repository.milestone.MilestoneRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MilestoneLayoutService {

    private static final double PADDING = 10.0;
    private static final double GHOST_PADDING = 16.0;
    private static final double MIN_DIST_Y = 20.0;
    private static final double STEP_SIZE = 22.0;
    private static final double X_NUDGE = 4.0;
    private static final double Y_WOBBLE = 2.0;
    private static final double MIN_Y_RANGE = 90.0;
    private static final int PUSH_PASSES = 40;
    private static final int CROSSING_PASSES = 10;

    private final MilestoneRepository milestoneRepository;
    private final MilestonePrerequisiteLinkRepository prerequisiteLinkRepository;

    public record Point(double x, double y) {
    }

    @Transactional
    public Map<UUID, Point> relayoutSet(UUID setId) {
        List<Milestone> milestones = milestoneRepository.findByMilestoneSet_IdAndActiveTrue(setId);
        if (milestones.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Point> positions = layout(milestones,
                prerequisiteLinkRepository.findBySetIdWithPrerequisites(setId));
        for (Milestone milestone : milestones) {
            Point point = positions.get(milestone.getId());
            if (point != null) {
                milestone.setPositionX(point.x());
                milestone.setPositionY(point.y());
            }
        }
        milestoneRepository.saveAll(milestones);
        log.info("Re-laid out {} milestones in set {}", milestones.size(), setId);
        return positions;
    }

    public Map<UUID, Point> layout(List<Milestone> milestones, List<MilestonePrerequisiteLink> links) {
        List<Milestone> sorted = new ArrayList<>(milestones);
        sorted.sort(Comparator.comparingInt(m -> tierOrder(m.getTier())));

        Set<UUID> local = new HashSet<>();
        for (Milestone m : sorted) {
            local.add(m.getId());
        }

        Map<UUID, List<UUID>> parentsOf = new HashMap<>();
        int ghosts = 0;
        for (MilestonePrerequisiteLink link : links) {
            UUID childId = link.getMilestone().getId();
            UUID parentId = link.getPrerequisiteMilestone().getId();
            if (!local.contains(childId)) {
                continue;
            }
            parentsOf.computeIfAbsent(childId, k -> new ArrayList<>()).add(parentId);
            if (!local.contains(parentId)) {
                ghosts++;
            }
        }

        double padding = ghosts > 0 ? GHOST_PADDING : PADDING;
        Map<UUID, Point> positions = parentsOf.isEmpty()
                ? radial(sorted, padding)
                : tree(sorted, local, parentsOf, padding);
        return normalize(positions, padding);
    }

    private Map<UUID, Point> radial(List<Milestone> sorted, double padding) {
        Map<UUID, Point> positions = new LinkedHashMap<>();
        double radius = Math.max(30.0, sorted.size() * MIN_DIST_Y / (2 * Math.PI));
        for (int i = 0; i < sorted.size(); i++) {
            UUID id = sorted.get(i).getId();
            double angle = (2 * Math.PI * i) / sorted.size() - Math.PI / 2;
            double r = radius + seededRandom(hashString(id.toString())) * 6 - 3;
            positions.put(id, new Point(radius + padding + r * Math.cos(angle),
                    radius + padding + r * Math.sin(angle)));
        }
        return positions;
    }

    private Map<UUID, Point> tree(List<Milestone> sorted, Set<UUID> local,
            Map<UUID, List<UUID>> parentsOf, double padding) {
        Map<UUID, Integer> depths = new HashMap<>();
        for (Milestone m : sorted) {
            resolveDepth(m.getId(), local, parentsOf, depths, new HashSet<>());
        }
        int maxDepth = depths.values().stream().mapToInt(Integer::intValue).max().orElse(0);

        Map<UUID, UUID> primaryParent = new HashMap<>();
        for (Milestone m : sorted) {
            List<UUID> parents = localParents(m.getId(), local, parentsOf);
            if (parents.isEmpty()) {
                continue;
            }
            UUID best = parents.get(0);
            for (UUID candidate : parents) {
                if (depths.getOrDefault(candidate, 0) > depths.getOrDefault(best, 0)) {
                    best = candidate;
                }
            }
            primaryParent.put(m.getId(), best);
        }

        Map<UUID, List<UUID>> childrenOf = new HashMap<>();
        for (Map.Entry<UUID, UUID> entry : primaryParent.entrySet()) {
            childrenOf.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
        }

        Map<UUID, Integer> weights = new HashMap<>();
        for (Milestone m : sorted) {
            resolveWeight(m.getId(), local, childrenOf, weights);
        }
        for (List<UUID> children : childrenOf.values()) {
            children.sort(Comparator.comparingInt((UUID id) -> weights.getOrDefault(id, 1)).reversed());
        }

        List<Milestone> roots = sorted.stream()
                .filter(m -> localParents(m.getId(), local, parentsOf).isEmpty())
                .toList();

        Map<Integer, Integer> layerSizes = new HashMap<>();
        for (Milestone m : sorted) {
            layerSizes.merge(depths.getOrDefault(m.getId(), 0), 1, Integer::sum);
        }
        int widestLayer = layerSizes.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        double yRange = Math.max(MIN_Y_RANGE, widestLayer * MIN_DIST_Y);
        double yMaxBound = padding + yRange;
        double xMaxBound = padding + Math.max(maxDepth, 1) * STEP_SIZE + X_NUDGE;

        Map<UUID, Point> positions = new LinkedHashMap<>();
        List<List<UUID>> components = components(sorted, roots, local, childrenOf, weights);

        int totalWeight = 0;
        for (List<UUID> component : components) {
            for (UUID rootId : component) {
                totalWeight += weights.getOrDefault(rootId, 1);
            }
        }

        double cursor = padding;
        for (List<UUID> componentRoots : components) {
            int componentWeight = 0;
            for (UUID rootId : componentRoots) {
                componentWeight += weights.getOrDefault(rootId, 1);
            }
            double slice = (yRange * componentWeight) / Math.max(totalWeight, 1);
            double rootCursor = cursor;
            for (UUID rootId : componentRoots) {
                double rootSlice = (slice * weights.getOrDefault(rootId, 1)) / Math.max(componentWeight, 1);
                place(rootId, padding - STEP_SIZE, rootCursor, rootCursor + rootSlice,
                        local, childrenOf, weights, positions, padding, xMaxBound, yMaxBound);
                rootCursor += rootSlice;
            }
            cursor += slice;
        }

        for (Milestone m : sorted) {
            if (!positions.containsKey(m.getId())) {
                int h = hashString(m.getId().toString());
                positions.put(m.getId(), new Point(padding + seededRandom(h) * STEP_SIZE,
                        padding + seededRandom(h + 1) * yRange));
            }
        }

        pushApart(positions);
        reduceCrossings(positions, sorted, depths, parentsOf);
        return positions;
    }

    private List<List<UUID>> components(List<Milestone> sorted, List<Milestone> roots, Set<UUID> local,
            Map<UUID, List<UUID>> childrenOf, Map<UUID, Integer> weights) {
        List<List<UUID>> components = new ArrayList<>();
        Set<UUID> assigned = new HashSet<>();
        for (Milestone root : roots) {
            if (assigned.contains(root.getId())) {
                continue;
            }
            Set<UUID> members = reachable(root.getId(), local, childrenOf);
            assigned.addAll(members);
            List<UUID> componentRoots = roots.stream()
                    .map(Milestone::getId)
                    .filter(members::contains)
                    .toList();
            components.add(new ArrayList<>(componentRoots));
        }
        for (Milestone m : sorted) {
            if (assigned.add(m.getId())) {
                components.add(new ArrayList<>(List.of(m.getId())));
            }
        }
        components.sort(Comparator.comparingInt(
                (List<UUID> c) -> c.stream().mapToInt(id -> weights.getOrDefault(id, 1)).sum()).reversed());
        return components;
    }

    private Set<UUID> reachable(UUID rootId, Set<UUID> local, Map<UUID, List<UUID>> childrenOf) {
        Set<UUID> seen = new HashSet<>();
        Deque<UUID> queue = new ArrayDeque<>();
        queue.push(rootId);
        while (!queue.isEmpty()) {
            UUID id = queue.pop();
            if (!local.contains(id) || !seen.add(id)) {
                continue;
            }
            for (UUID child : childrenOf.getOrDefault(id, List.of())) {
                if (local.contains(child)) {
                    queue.push(child);
                }
            }
        }
        return seen;
    }

    private void place(UUID id, double parentX, double yMin, double yMax, Set<UUID> local,
            Map<UUID, List<UUID>> childrenOf, Map<UUID, Integer> weights, Map<UUID, Point> positions,
            double padding, double xMaxBound, double yMaxBound) {
        int h = hashString(id.toString());
        double x = clamp(parentX + STEP_SIZE + (seededRandom(h) - 0.5) * X_NUDGE, padding, xMaxBound);
        double y = clamp((yMin + yMax) / 2 + (seededRandom(h + 7) - 0.5) * Y_WOBBLE, padding, yMaxBound);
        positions.put(id, new Point(x, y));

        List<UUID> children = childrenOf.getOrDefault(id, List.of()).stream().filter(local::contains).toList();
        if (children.isEmpty()) {
            return;
        }
        int totalWeight = children.stream().mapToInt(c -> weights.getOrDefault(c, 1)).sum();
        double cursor = yMin;
        for (UUID child : children) {
            double slice = ((yMax - yMin) * weights.getOrDefault(child, 1)) / Math.max(totalWeight, 1);
            place(child, x, cursor, cursor + slice, local, childrenOf, weights, positions,
                    padding, xMaxBound, yMaxBound);
            cursor += slice;
        }
    }

    private void pushApart(Map<UUID, Point> positions) {
        List<UUID> ids = new ArrayList<>(positions.keySet());
        for (int pass = 0; pass < PUSH_PASSES; pass++) {
            boolean pushed = false;
            for (int i = 0; i < ids.size(); i++) {
                for (int j = i + 1; j < ids.size(); j++) {
                    Point a = positions.get(ids.get(i));
                    Point b = positions.get(ids.get(j));
                    double dy = Math.abs(b.y() - a.y());
                    double dx = Math.abs(b.x() - a.x());
                    if (dy >= MIN_DIST_Y || dx > STEP_SIZE * 0.8) {
                        continue;
                    }
                    pushed = true;
                    double push = (MIN_DIST_Y - dy) / 2 + 0.5;
                    boolean aFirst = a.y() <= b.y();
                    positions.put(ids.get(i), new Point(a.x(), aFirst ? a.y() - push : a.y() + push));
                    positions.put(ids.get(j), new Point(b.x(), aFirst ? b.y() + push : b.y() - push));
                }
            }
            if (!pushed) {
                return;
            }
        }
    }

    private void reduceCrossings(Map<UUID, Point> positions, List<Milestone> sorted,
            Map<UUID, Integer> depths, Map<UUID, List<UUID>> parentsOf) {
        List<UUID[]> edges = new ArrayList<>();
        for (Map.Entry<UUID, List<UUID>> entry : parentsOf.entrySet()) {
            for (UUID parentId : entry.getValue()) {
                if (positions.containsKey(parentId) && positions.containsKey(entry.getKey())) {
                    edges.add(new UUID[] { parentId, entry.getKey() });
                }
            }
        }
        if (edges.isEmpty()) {
            return;
        }

        Map<Integer, List<UUID>> depthGroups = new LinkedHashMap<>();
        for (Milestone m : sorted) {
            depthGroups.computeIfAbsent(depths.getOrDefault(m.getId(), 0), k -> new ArrayList<>()).add(m.getId());
        }

        for (int pass = 0; pass < CROSSING_PASSES; pass++) {
            boolean improved = false;
            for (List<UUID> group : depthGroups.values()) {
                for (int i = 0; i < group.size(); i++) {
                    for (int j = i + 1; j < group.size(); j++) {
                        int before = countCrossings(edges, positions);
                        swapY(positions, group.get(i), group.get(j));
                        if (countCrossings(edges, positions) < before) {
                            improved = true;
                        } else {
                            swapY(positions, group.get(i), group.get(j));
                        }
                    }
                }
            }
            if (!improved) {
                return;
            }
        }
    }

    private void swapY(Map<UUID, Point> positions, UUID a, UUID b) {
        Point pa = positions.get(a);
        Point pb = positions.get(b);
        positions.put(a, new Point(pa.x(), pb.y()));
        positions.put(b, new Point(pb.x(), pa.y()));
    }

    private int countCrossings(List<UUID[]> edges, Map<UUID, Point> positions) {
        int count = 0;
        for (int i = 0; i < edges.size(); i++) {
            double fromA = positions.get(edges.get(i)[0]).y();
            double toA = positions.get(edges.get(i)[1]).y();
            for (int j = i + 1; j < edges.size(); j++) {
                double d1 = fromA - positions.get(edges.get(j)[0]).y();
                double d2 = toA - positions.get(edges.get(j)[1]).y();
                if ((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) {
                    count++;
                }
            }
        }
        return count;
    }

    private Map<UUID, Point> normalize(Map<UUID, Point> positions, double padding) {
        if (positions.isEmpty()) {
            return positions;
        }
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        for (Point p : positions.values()) {
            minX = Math.min(minX, p.x());
            minY = Math.min(minY, p.y());
        }
        Map<UUID, Point> shifted = new LinkedHashMap<>();
        for (Map.Entry<UUID, Point> entry : positions.entrySet()) {
            shifted.put(entry.getKey(), new Point(
                    round(entry.getValue().x() - minX + padding),
                    round(entry.getValue().y() - minY + padding)));
        }
        return shifted;
    }

    private int resolveDepth(UUID id, Set<UUID> local, Map<UUID, List<UUID>> parentsOf,
            Map<UUID, Integer> depths, Set<UUID> visiting) {
        if (!visiting.add(id)) {
            return 0;
        }
        Integer cached = depths.get(id);
        if (cached != null) {
            return cached;
        }
        int depth = 0;
        for (UUID parentId : localParents(id, local, parentsOf)) {
            depth = Math.max(depth, 1 + resolveDepth(parentId, local, parentsOf, depths, new HashSet<>(visiting)));
        }
        depths.put(id, depth);
        return depth;
    }

    private int resolveWeight(UUID id, Set<UUID> local, Map<UUID, List<UUID>> childrenOf,
            Map<UUID, Integer> weights) {
        Integer cached = weights.get(id);
        if (cached != null) {
            return cached;
        }
        weights.put(id, 1);
        List<UUID> children = childrenOf.getOrDefault(id, List.of()).stream().filter(local::contains).toList();
        int weight = 0;
        for (UUID child : children) {
            weight += resolveWeight(child, local, childrenOf, weights);
        }
        int resolved = children.isEmpty() ? 1 : weight;
        weights.put(id, resolved);
        return resolved;
    }

    private List<UUID> localParents(UUID id, Set<UUID> local, Map<UUID, List<UUID>> parentsOf) {
        return parentsOf.getOrDefault(id, List.of()).stream().filter(local::contains).toList();
    }

    private int tierOrder(MilestoneTier tier) {
        return tier == null ? 0 : tier.ordinal();
    }

    private double clamp(double value, double min, double max) {
        return Math.min(max, Math.max(min, value));
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private static int hashString(String value) {
        int hash = 0;
        for (int i = 0; i < value.length(); i++) {
            hash = (hash << 5) - hash + value.charAt(i);
        }
        return Math.abs(hash);
    }

    private static double seededRandom(int seed) {
        double x = Math.sin(seed * 9301.0 + 49297.0) * 49297.0;
        return x - Math.floor(x);
    }
}
