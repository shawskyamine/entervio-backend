package com.entrevio.entrevio_backend.dto.job;

import com.entrevio.entrevio_backend.model.Job;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDto {
    private UUID id;
    private String title;
    private String description;
    private String location;
    private String employmentType;
    private String status;
    private String companyName;
    private ParsedJob parsed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static JobDto from(Job job, ParsedJob parsed) {
        return JobDto.builder()
                .id(job.getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .location(job.getLocation())
                .employmentType(job.getEmploymentType() != null
                        ? job.getEmploymentType().name().toLowerCase()
                        : null)
                .status(job.getStatus().name().toLowerCase())
                .companyName(job.getCompany() != null ? job.getCompany().getName() : null)
                .parsed(parsed)
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}