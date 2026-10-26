package com.entrevio.entrevio_backend.controller;

import com.entrevio.entrevio_backend.dto.ApiResponse;
import com.entrevio.entrevio_backend.dto.rag.CvRagSearchRequest;
import com.entrevio.entrevio_backend.dto.rag.CvRagSearchResponse;
import com.entrevio.entrevio_backend.service.CvRagService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cv/rag")
@PreAuthorize("hasRole('RECRUITER')")
public class CvRagController {

    private final CvRagService cvRagService;

    public CvRagController(CvRagService cvRagService) {
        this.cvRagService = cvRagService;
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<CvRagSearchResponse>> search(
            @Valid @RequestBody CvRagSearchRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(cvRagService.search(req)));
    }

    /** Backfill — index every parsed CV that isn't indexed yet. */
    @PostMapping("/reindex")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reindex() {
        int count = cvRagService.reindexAll();
        return ResponseEntity.ok(ApiResponse.ok(Map.of("indexed", count)));
    }
}