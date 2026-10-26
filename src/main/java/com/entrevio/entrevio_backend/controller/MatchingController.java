package com.entrevio.entrevio_backend.controller;

import com.entrevio.entrevio_backend.dto.ApiResponse;
import com.entrevio.entrevio_backend.dto.matching.MatchDto;
import com.entrevio.entrevio_backend.service.MatchingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/matching")
public class MatchingController {

    private final MatchingService matchingService;

    public MatchingController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    /** Recruiter: recompute matches for one job. */
    @PostMapping("/jobs/{jobId}/run")
    public ResponseEntity<ApiResponse<List<MatchDto>>> run(@PathVariable UUID jobId) {
        return ResponseEntity.ok(ApiResponse.ok(matchingService.runForJob(jobId)));
    }

    /** Recruiter: get existing matches (does not recompute). */
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<ApiResponse<List<MatchDto>>> listForJob(@PathVariable UUID jobId) {
        return ResponseEntity.ok(ApiResponse.ok(matchingService.listForJob(jobId)));
    }

    /** Candidate: my matches across all jobs. */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<MatchDto>>> listMine() {
        return ResponseEntity.ok(ApiResponse.ok(matchingService.listMine()));
    }
}