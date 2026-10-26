package com.entrevio.entrevio_backend.dto.rag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CvRagSearchRequest {

    @NotBlank
    @Size(min = 3, max = 500)
    private String query;

    /** Optional override — defaults to entrevio.rag.top-k. */
    private Integer topK;
}