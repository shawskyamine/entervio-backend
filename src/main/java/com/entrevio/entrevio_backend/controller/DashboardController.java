package com.entrevio.entrevio_backend.controller;

import com.entrevio.entrevio_backend.dto.ApiResponse;
import com.entrevio.entrevio_backend.dto.dashboard.PipelineDto;
import com.entrevio.entrevio_backend.dto.dashboard.RecruiterStatsDto;
import com.entrevio.entrevio_backend.dto.dashboard.TimelinePointDto;
import com.entrevio.entrevio_backend.dto.dashboard.TopJobDto;
import com.entrevio.entrevio_backend.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/recruiter/stats")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<RecruiterStatsDto>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getRecruiterStats()));
    }

    @GetMapping("/recruiter/timeline")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<List<TimelinePointDto>>> getTimeline(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getTimeline(days)));
    }

    @GetMapping("/recruiter/pipeline")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<PipelineDto>> getPipeline() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getPipeline()));
    }

    @GetMapping("/recruiter/top-jobs")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<List<TopJobDto>>> getTopJobs(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getTopJobs(limit)));
    }

    @GetMapping("/recruiter/summary")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSummary() {
        var stats = dashboardService.getRecruiterStats();
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "activeJobsCount",   stats.getActiveJobs(),
                "applicantsCount",   stats.getTotalApplications(),
                "interviewsCount",   stats.getInterviewsCount(),
                "matchesReadyCount", stats.getAcceptedCount()
        )));
    }
}