package com.entrevio.entrevio_backend.controller;

import com.entrevio.entrevio_backend.dto.ApiResponse;
import com.entrevio.entrevio_backend.dto.job.CreateJobRequest;
import com.entrevio.entrevio_backend.dto.job.JobDto;
import com.entrevio.entrevio_backend.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    /** Public browse — only OPEN jobs. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<JobDto>>> listOpen() {
        return ResponseEntity.ok(ApiResponse.ok(jobService.listOpen()));
    }

    /** Recruiter view — my company's jobs (any status). */
    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<JobDto>>> listMine() {
        return ResponseEntity.ok(ApiResponse.ok(jobService.listMine()));
    }

    /** Get one job by ID. */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobDto>> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(jobService.getById(id)));
    }

    /** Create a job (recruiter only). */
    @PostMapping
    public ResponseEntity<ApiResponse<JobDto>> create(
            @Valid @RequestBody CreateJobRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(jobService.create(req)));
    }

    /** Update status (open / closed / draft). */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<JobDto>> updateStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");
        return ResponseEntity.ok(ApiResponse.ok(jobService.updateStatus(id, newStatus)));
    }

    /** Delete (recruiter only). */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        jobService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}