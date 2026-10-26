package com.entrevio.entrevio_backend.controller;

import com.entrevio.entrevio_backend.dto.ApiResponse;
import com.entrevio.entrevio_backend.dto.recruiter.CompanyDto;
import com.entrevio.entrevio_backend.dto.recruiter.UpdateCompanyRequest;
import com.entrevio.entrevio_backend.service.RecruiterService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recruiter/company")
public class RecruiterController {

    private final RecruiterService recruiterService;

    public RecruiterController(RecruiterService recruiterService) {
        this.recruiterService = recruiterService;
    }

    @GetMapping
    public ApiResponse<CompanyDto> get() {
        return ApiResponse.ok(recruiterService.getOrCreateMine());
    }

    @PutMapping
    public ApiResponse<CompanyDto> update(@Valid @RequestBody UpdateCompanyRequest req) {
        return ApiResponse.ok(recruiterService.updateMine(req));
    }
}