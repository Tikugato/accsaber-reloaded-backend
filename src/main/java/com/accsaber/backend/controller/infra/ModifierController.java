package com.accsaber.backend.controller.infra;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.accsaber.backend.model.dto.response.ModifierResponse;
import com.accsaber.backend.service.infra.ModifierService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/modifiers")
@RequiredArgsConstructor
@Tag(name = "Platform")
public class ModifierController {

    private final ModifierService modifierService;

    @Operation(summary = "List the score modifiers", description = "Every modifier currently in use, each with its short code "
            + "like NF or DA and the multiplier it applies to a score. Scores point at these by id rather than by code, so it is "
            + "worth pulling this once and keeping it around instead of looking one up each time.")
    @GetMapping
    public ResponseEntity<List<ModifierResponse>> listModifiers() {
        return ResponseEntity.ok(modifierService.findAllActive());
    }
}
