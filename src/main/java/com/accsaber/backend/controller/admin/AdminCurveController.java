package com.accsaber.backend.controller.admin;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.accsaber.backend.model.dto.request.curve.CreateCurveRequest;
import com.accsaber.backend.model.dto.request.curve.UpdateCurveRequest;
import com.accsaber.backend.model.dto.response.CurveResponse;
import com.accsaber.backend.service.infra.CurveService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/admin/curves")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Curves")
public class AdminCurveController {

    private final CurveService curveService;

    @Operation(summary = "Create a curve")
    @PostMapping
    public ResponseEntity<CurveResponse> createCurve(@Valid @RequestBody CreateCurveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(curveService.createCurve(request));
    }

    @Operation(summary = "Update a curve")
    @PatchMapping("/{id}")
    public ResponseEntity<CurveResponse> updateCurve(@PathVariable UUID id,
            @Valid @RequestBody UpdateCurveRequest request) {
        return ResponseEntity.ok(curveService.updateCurve(id, request));
    }
}
