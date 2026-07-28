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
@Tag(name = "Platform")
public class CurveController {

    private final CurveService curveService;

    @Operation(summary = "List the scoring curves", description = "Curves come in two shapes. A point lookup curve is a set of "
            + "accuracy to AP points that get interpolated between, which is what turns an accuracy into AP. A formula curve "
            + "carries its parameters instead and handles the weighting that gives your later scores less pull. "
            + "This gives you all the active ones.")
    @GetMapping
    public ResponseEntity<List<CurveResponse>> getAllCurves() {
        return ResponseEntity.ok(curveService.findAllActive());
    }

    @Operation(summary = "Get one curve", description = "A single curve by id, points included if it is a point lookup one. "
            + "There are around a thousand points on each of the AP curves, so please do not fetch this every time you want to "
            + "work out a score. Read it once and hold on to it.")
    @GetMapping("/{id}")
    public ResponseEntity<CurveResponse> getCurve(@PathVariable UUID id) {
        return ResponseEntity.ok(curveService.findById(id));
    }
}
