package com.entrevio.entrevio_backend.dto.job;

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
public class CreateJobRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    @Size(min = 50, max = 20000)
    private String description;

    @Size(max = 200)
    private String location;

    /** "FULL_TIME" | "PART_TIME" | "CONTRACT" | "INTERNSHIP" */
    private String employmentType;
}