package com.accsaber.backend.controller.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.accsaber.backend.model.dto.response.player.ApToNextResponse;
import com.accsaber.backend.model.dto.response.player.SkillResponse;
import com.accsaber.backend.service.skill.SkillService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Tag(name = "Players")
public class SkillController {

    private final SkillService skillService;

    @Operation(summary = "Get a player's skill levels", description = "A 0 to 100 skill level per category, worked out from "
            + "three things: where they rank, what they can sustain, and their single best play. The components come back "
            + "separately as well as combined, which is what you want if you are drawing a radar chart rather than showing one "
            + "number. Peak is measured against the highest AP available in that category, so it moves when the category "
            + "does. You get every category unless you pass one.")
    @GetMapping("/{userId}/skill")
    public ResponseEntity<SkillResponse> getSkill(
            @PathVariable Long userId,
            @Parameter(description = "Optional category code; omit for all categories") @RequestParam(required = false) String category) {
        return ResponseEntity.ok(skillService.computeSkillForUser(userId, category));
    }

    @Operation(summary = "Work out what a player needs for one more AP", description = "The raw AP a single new play would have "
            + "to be worth to move this player's weighted total in a category up by exactly one. Because weighting gives your "
            + "later scores less pull, this number climbs steeply the better someone already is, which is rather the point of "
            + "showing it.")
    @GetMapping("/{userId}/categories/{category}/ap-to-next")
    public ResponseEntity<ApToNextResponse> getApToNext(
            @PathVariable Long userId,
            @PathVariable String category) {
        return ResponseEntity.ok(skillService.calculateApToNext(userId, category));
    }
}
