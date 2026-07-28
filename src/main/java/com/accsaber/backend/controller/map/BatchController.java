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

import com.accsaber.backend.model.dto.response.map.PublicBatchResponse;
import com.accsaber.backend.service.map.BatchService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/batches")
@RequiredArgsConstructor
@Tag(name = "Maps")
public class BatchController {

    private final BatchService batchService;

    @Operation(summary = "List the released batches", description = "A batch is a set of difficulties the ranking team put "
            + "together and released as one, so this is effectively the release history, newest first. Only batches that have "
            + "actually gone out show up here, since anything still being prepared is not public yet. Search by name if you are "
            + "after a particular one.")
    @GetMapping
    public ResponseEntity<Page<PublicBatchResponse>> listBatches(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(batchService.findAllPublic(search, pageable));
    }

    @Operation(summary = "Get one released batch", description = "A single batch with every difficulty that went out in it. A "
            + "batch that has not been released yet comes back as a 404 rather than an empty one, so do not read that as the "
            + "batch not existing.")
    @GetMapping("/{id}")
    public ResponseEntity<PublicBatchResponse> getBatch(@PathVariable UUID id) {
        return ResponseEntity.ok(batchService.findByIdPublic(id));
    }
}
