package com.entrevio.entrevio_backend.controller;

import com.entrevio.entrevio_backend.dto.ApiResponse;
import com.entrevio.entrevio_backend.dto.candidate.CandidateProfileDto;
import com.entrevio.entrevio_backend.dto.candidate.UpdateCandidateProfileRequest;
import com.entrevio.entrevio_backend.service.CandidateService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/candidate/profile")
public class CandidateController {

    private final CandidateService candidateService;

    public CandidateController(CandidateService candidateService) {
        this.candidateService = candidateService;
    }

    @GetMapping
    public ApiResponse<CandidateProfileDto> get() {
        return ApiResponse.ok(candidateService.getOrCreateMine());
    }

    @PutMapping
    public ApiResponse<CandidateProfileDto> update(@Valid @RequestBody UpdateCandidateProfileRequest req) {
        return ApiResponse.ok(candidateService.updateMine(req));
    }
}