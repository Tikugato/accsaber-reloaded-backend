package com.accsaber.backend.controller.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.accsaber.backend.exception.ResourceNotFoundException;
import com.accsaber.backend.model.dto.request.admin.RunJobRequest;
import com.accsaber.backend.model.dto.response.admin.JobResponse;
import com.accsaber.backend.service.admin.AdminJobService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/admin/jobs")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin - Operations")
public class AdminJobController {

    private final AdminJobService jobService;

    @Operation(summary = "Start a background job", description = "Kicks off one of the heavy maintenance jobs and hands you "
            + "back its id straight away. Which extra fields you need depends on the type, and you get a 422 telling you which "
            + "one is missing if you leave it out. Use the id with the routes below to see whether it finished or blew up. "
            + "Nothing is queued, so starting the same job twice really does run it twice.")
    @PostMapping
    public ResponseEntity<JobResponse> run(@Valid @RequestBody RunJobRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(JobResponse.from(jobService.run(request)));
    }

    @Operation(summary = "List jobs", description = "Everything running right now, newest first, followed by the last 50 that "
            + "finished. This is held in memory rather than the database, so a restart clears it, which is fine because a "
            + "restart also kills whatever was running.")
    @GetMapping
    public ResponseEntity<List<JobResponse>> list() {
        return ResponseEntity.ok(jobService.list().stream().map(JobResponse::from).toList());
    }

    @Operation(summary = "Get one job", description = "The state of a single job, including the error message if it failed. "
            + "Jobs drop off the end of the recent list eventually, so an id that used to work can start coming back as a 404.")
    @GetMapping("/{jobId}")
    public ResponseEntity<JobResponse> get(@PathVariable UUID jobId) {
        return ResponseEntity.ok(JobResponse.from(jobService.find(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId))));
    }
}
