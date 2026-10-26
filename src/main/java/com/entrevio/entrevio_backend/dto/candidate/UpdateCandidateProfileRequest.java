package com.entrevio.entrevio_backend.dto.candidate;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCandidateProfileRequest {

    @Size(max = 255)
    private String headline;

    @Size(max = 255)
    private String location;

    private Integer yearsExperience;

    private String skills;
}