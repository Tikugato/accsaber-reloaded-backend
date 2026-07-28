package com.accsaber.backend.controller.milestone;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.accsaber.backend.model.dto.response.milestone.MilestoneCompletionResponse;
import com.accsaber.backend.model.dto.response.milestone.MilestoneHolderResponse;
import com.accsaber.backend.model.dto.response.milestone.MilestoneResponse;
import com.accsaber.backend.model.dto.response.milestone.MilestoneSetGroupResponse;
import com.accsaber.backend.model.dto.response.milestone.MilestoneSetLinkResponse;
import com.accsaber.backend.model.dto.response.milestone.MilestoneSetResponse;
import com.accsaber.backend.model.dto.response.milestone.PrerequisiteLinkResponse;
import com.accsaber.backend.model.entity.milestone.LevelThreshold;
import com.accsaber.backend.service.infra.CategoryService;
import com.accsaber.backend.service.milestone.LevelService;
import com.accsaber.backend.service.milestone.MilestoneService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "Milestones")
public class MilestoneController {

    private final MilestoneService milestoneService;
    private final LevelService levelService;
    private final CategoryService categoryService;

    @Operation(summary = "List the milestones", description = "A page of every milestone currently live. Narrow it to one set, "
            + "to a category by UUID or code, or to a type. Type is either milestone for the general progress goals or "
            + "achievement for the one off novelty ones. This is the catalogue rather than anyone's progress, so for that have "
            + "a look at the player milestone routes.")
    @GetMapping("/milestones")
    public ResponseEntity<Page<MilestoneResponse>> listMilestones(
            @RequestParam(required = false) UUID setId,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String type,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity
                .ok(milestoneService.findAllActive(setId, categoryService.resolveId(categoryId), type, pageable));
    }

    @Operation(summary = "List the milestone sets", description = "Milestones are grouped into sets, and finishing a whole set "
            + "can pay out bonus XP on top of the individual ones. Pass userId if you want each set to come back with how far "
            + "that player has got through it.")
    @GetMapping("/milestones/sets")
    public ResponseEntity<Page<MilestoneSetResponse>> listMilestoneSets(
            @RequestParam(required = false) Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(milestoneService.findAllSets(userId, pageable));
    }

    @Operation(summary = "Get one milestone", description = "A single milestone with its title, what it asks for, and the XP it "
            + "pays out.")
    @GetMapping("/milestones/{id}")
    public ResponseEntity<MilestoneResponse> getMilestone(@PathVariable UUID id) {
        return ResponseEntity.ok(milestoneService.findById(id));
    }

    @Operation(summary = "List who has earned a milestone", description = "The players who have completed one, most recent "
            + "first. Rarer milestones make for a short list, which is rather the point of them.")
    @GetMapping("/milestones/{id}/holders")
    public ResponseEntity<Page<MilestoneHolderResponse>> getMilestoneHolders(
            @PathVariable UUID id,
            @PageableDefault(size = 20, sort = "completedAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(milestoneService.findMilestoneHolders(id, pageable));
    }

    @Operation(summary = "List the milestones in a set", description = "Everything belonging to one set, as a flat list rather "
            + "than a page since sets are not usually big enough to need paging.")
    @GetMapping("/milestones/sets/{setId}/milestones")
    public ResponseEntity<List<MilestoneResponse>> getMilestonesBySet(@PathVariable UUID setId) {
        return ResponseEntity.ok(milestoneService.findBySet(setId));
    }

    @Operation(summary = "Get the prerequisites inside a set", description = "Some milestones in a set only open up once another "
            + "one is done. This gives you those links so you can draw the set as a tree rather than a flat list.")
    @GetMapping("/milestones/sets/{setId}/prerequisites")
    public ResponseEntity<List<PrerequisiteLinkResponse>> getPrerequisiteLinksBySet(@PathVariable UUID setId) {
        return ResponseEntity.ok(milestoneService.findPrerequisiteLinksBySet(setId));
    }

    @Operation(summary = "List the set groups", description = "Sets can themselves be gathered into groups, which is the layer "
            + "above sets and is mostly there for how they get presented.")
    @GetMapping("/milestones/set-groups")
    public ResponseEntity<List<MilestoneSetGroupResponse>> listSetGroups() {
        return ResponseEntity.ok(milestoneService.findAllActiveGroups());
    }

    @Operation(summary = "Get the sets in a group", description = "Which sets belong to one group, and in what order they are "
            + "meant to appear.")
    @GetMapping("/milestones/set-groups/{groupId}/links")
    public ResponseEntity<List<MilestoneSetLinkResponse>> getSetLinksByGroup(@PathVariable UUID groupId) {
        return ResponseEntity.ok(milestoneService.findSetLinksByGroup(groupId));
    }

    @Operation(summary = "Get the groups a set belongs to", description = "The other way round from the route above, so if you "
            + "are looking at one set this tells you where it sits. A set can be in more than one group.")
    @GetMapping("/milestones/sets/{setId}/groups")
    public ResponseEntity<List<MilestoneSetLinkResponse>> getSetLinksBySet(@PathVariable UUID setId) {
        return ResponseEntity.ok(milestoneService.findSetLinksBySet(setId));
    }

    @Operation(summary = "Get how many people have each milestone", description = "Completion counts across every live "
            + "milestone, which is how you work out which ones are actually rare. Pass userId to get that player's own state "
            + "alongside each count. Sort defaults to tier.")
    @GetMapping("/milestones/completion-stats")
    public ResponseEntity<List<MilestoneCompletionResponse>> getCompletionStats(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "tier") String sort) {
        return ResponseEntity.ok(milestoneService.findAllCompletionStats(userId, sort));
    }

    @Operation(summary = "List the level thresholds", description = "How much total XP each level needs, and the title that goes "
            + "with it where there is one. These are configurable rather than following a formula, so read them from here "
            + "instead of trying to derive them.")
    @GetMapping("/levels")
    public ResponseEntity<List<LevelThreshold>> listLevels() {
        return ResponseEntity.ok(levelService.getAllThresholds());
    }
}
