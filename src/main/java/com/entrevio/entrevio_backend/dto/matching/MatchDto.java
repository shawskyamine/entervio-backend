package com.entrevio.entrevio_backend.dto.matching;

import com.entrevio.entrevio_backend.model.Match;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchDto {
    private UUID id;
    private Integer score;
    private MatchBreakdown breakdown;
    private String explanation;
    private boolean llmGenerated;

    private UUID candidateId;
    private String candidateName;
    private String candidateEmail;
    private String candidateHeadline;
    private Integer candidateYearsExperience;

    public static MatchDto from(Match m, MatchBreakdown breakdown) {
        return MatchDto.builder()
                .id(m.getId())
                .score(m.getScore())
                .breakdown(breakdown)
                .explanation(m.getExplanation())
                .llmGenerated(m.isLlmGenerated())
                .candidateId(m.getCandidate().getId())
                .candidateName(m.getCandidate().getFullName())
                .candidateEmail(m.getCandidate().getEmail())
                .candidateHeadline(
                        m.getCandidate().getCandidateProfile() != null
                                ? m.getCandidate().getCandidateProfile().getHeadline()
                                : null
                )
                .candidateYearsExperience(
                        m.getCandidate().getCandidateProfile() != null
                                ? m.getCandidate().getCandidateProfile().getYearsExperience()
                                : null
                )
                .build();
    }
}