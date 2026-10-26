package com.entrevio.entrevio_backend.dto.candidate;

import com.entrevio.entrevio_backend.model.CandidateProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateProfileDto {
    private UUID id;
    private UUID userId;
    private String fullName;
    private String email;
    private String headline;
    private String location;
    private Integer yearsExperience;
    private String skills;

    public static CandidateProfileDto from(CandidateProfile p) {
        return CandidateProfileDto.builder()
                .id(p.getId())
                .userId(p.getUser().getId())
                .fullName(p.getUser().getFullName())
                .email(p.getUser().getEmail())
                .headline(p.getHeadline())
                .location(p.getLocation())
                .yearsExperience(p.getYearsExperience())
                .skills(p.getSkills())
                .build();
    }
}