package com.accsaber.backend.controller.infra;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.accsaber.backend.model.dto.response.CurveResponse;
import com.accsaber.backend.service.infra.CurveService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/curves")
@RequiredArgsConstructor
@Tag(name = "Curves")
public class CurveController {

    private final CurveService curveService;

    @Operation(summary = "Get all active curves")
    @GetMapping
    public ResponseEntity<List<CurveResponse>> getAllCurves() {
        return ResponseEntity.ok(curveService.findAllActive());
    }

    @Operation(summary = "Get a curve by ID")
    @GetMapping("/{id}")
    public ResponseEntity<CurveResponse> getCurve(@PathVariable UUID id) {
        return ResponseEntity.ok(curveService.findById(id));
    }
}
