package com.accsaber.backend.controller.map;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.accsaber.backend.model.dto.response.map.BatchResponse;
import com.accsaber.backend.model.entity.map.BatchStatus;
import com.accsaber.backend.service.map.BatchService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/batches")
@RequiredArgsConstructor
@Tag(name = "Batches")
public class BatchController {

    private final BatchService batchService;

    @Operation(summary = "List batches", description = "Paginated batch list, optionally filtered by status")
    @GetMapping
    public ResponseEntity<Page<BatchResponse>> listBatches(
            @RequestParam(required = false) BatchStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<BatchResponse> result = status != null
                ? batchService.findByStatus(status, pageable)
                : batchService.findAll(pageable);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Get batch by ID", description = "Returns a batch with all its assigned map difficulties")
    @GetMapping("/{id}")
    public ResponseEntity<BatchResponse> getBatch(@PathVariable UUID id) {
        return ResponseEntity.ok(batchService.findById(id));
    }
}
