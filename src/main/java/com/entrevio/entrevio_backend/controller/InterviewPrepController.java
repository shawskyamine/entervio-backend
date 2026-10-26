package com.entrevio.entrevio_backend.controller;

import com.entrevio.entrevio_backend.dto.ApiResponse;
import com.entrevio.entrevio_backend.dto.interview.InterviewPrepResponse;
import com.entrevio.entrevio_backend.service.InterviewPrepService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
@PreAuthorize("hasRole('RECRUITER')")
public class InterviewPrepController {

    private final InterviewPrepService interviewPrepService;

    public InterviewPrepController(InterviewPrepService interviewPrepService) {
        this.interviewPrepService = interviewPrepService;
    }

    @PostMapping("/{id}/prepare-interview")
    public ResponseEntity<ApiResponse<InterviewPrepResponse>> prepare(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(interviewPrepService.prepare(id)));
    }
}