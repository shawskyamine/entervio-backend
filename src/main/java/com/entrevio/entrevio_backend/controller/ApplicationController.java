package com.entrevio.entrevio_backend.controller;

import com.entrevio.entrevio_backend.dto.ApiResponse;
import com.entrevio.entrevio_backend.dto.application.ApplicationDto;
import com.entrevio.entrevio_backend.dto.application.CreateApplicationRequest;
import com.entrevio.entrevio_backend.dto.application.UpdateApplicationStatusRequest;
import com.entrevio.entrevio_backend.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    // ---- Candidate ----

    @PostMapping
    public ResponseEntity<ApiResponse<ApplicationDto>> apply(
            @Valid @RequestBody CreateApplicationRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(applicationService.apply(req)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<ApplicationDto>>> listMine() {
        return ResponseEntity.ok(ApiResponse.ok(applicationService.listMine()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> withdraw(@PathVariable UUID id) {
        applicationService.withdraw(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ---- Recruiter ----

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<ApiResponse<List<ApplicationDto>>> listForJob(
            @PathVariable UUID jobId) {
        return ResponseEntity.ok(ApiResponse.ok(applicationService.listForJob(jobId)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ApplicationDto>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateApplicationStatusRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(applicationService.updateStatus(id, req)));
    }

    // ---- Shared ----

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ApplicationDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(applicationService.getById(id)));
    }
}