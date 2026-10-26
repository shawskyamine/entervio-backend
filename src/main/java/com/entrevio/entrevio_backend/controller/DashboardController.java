package com.entrevio.entrevio_backend.controller;

import com.entrevio.entrevio_backend.dto.ApiResponse;
import com.entrevio.entrevio_backend.model.User;
import com.entrevio.entrevio_backend.repository.CvRepository;
import com.entrevio.entrevio_backend.security.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final CvRepository cvRepository;

    public DashboardController(CvRepository cvRepository) {
        this.cvRepository = cvRepository;
    }

    @GetMapping("/candidate/summary")
    public ApiResponse<Map<String, Object>> candidateSummary() {
        User me = CurrentUser.get();
        boolean cvUploaded = cvRepository.existsByUserId(me.getId());
        return ApiResponse.ok(Map.of(
                "applicationsCount", 0,
                "interviewsCount",   0,
                "cvUploaded",        cvUploaded
        ));
    }

    @GetMapping("/recruiter/summary")
    public ApiResponse<Map<String, Object>> recruiterSummary() {
        return ApiResponse.ok(Map.of(
                "activeJobsCount",   0,
                "applicantsCount",   0,
                "interviewsCount",   0,
                "matchesReadyCount", 0
        ));
    }
}