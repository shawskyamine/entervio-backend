package com.entrevio.entrevio_backend.dto.application;

import com.entrevio.entrevio_backend.model.Application;
import com.entrevio.entrevio_backend.model.enums.CvStatus;
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
public class ApplicationDto {
    private UUID id;
    private String status;
    private String coverNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Job summary
    private UUID jobId;
    private String jobTitle;
    private String jobLocation;
    private String companyName;

    // Candidate summary
    private UUID candidateId;
    private String candidateName;
    private String candidateEmail;
    private String candidateHeadline;
    private Integer candidateYearsExperience;
    private boolean candidateHasParsedCv;

    public static ApplicationDto from(
            Application app,
            boolean hasParsedCv
    ) {
        var job = app.getJob();
        var cand = app.getCandidate();
        var profile = cand.getCandidateProfile();

        return ApplicationDto.builder()
                .id(app.getId())
                .status(app.getStatus().name().toLowerCase())
                .coverNote(app.getCoverNote())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())

                .jobId(job.getId())
                .jobTitle(job.getTitle())
                .jobLocation(job.getLocation())
                .companyName(job.getCompany() != null ? job.getCompany().getName() : null)

                .candidateId(cand.getId())
                .candidateName(cand.getFullName())
                .candidateEmail(cand.getEmail())
                .candidateHeadline(profile != null ? profile.getHeadline() : null)
                .candidateYearsExperience(profile != null ? profile.getYearsExperience() : null)
                .candidateHasParsedCv(hasParsedCv)

                .build();
    }
}