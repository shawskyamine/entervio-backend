package com.entrevio.entrevio_backend.dto.job;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedJob {

    // ---- Aligned with ParsedCv for Phase 5 matching ----
    private List<String> requiredSkills;       // ↔ ParsedCv.skills
    private List<String> preferredSkills;      // extra nice-to-haves
    private Integer minYearsExperience;        // ↔ ParsedCv.yearsExperience
    private String seniority;                  // "junior" | "mid" | "senior" | "lead"
    private String educationLevel;             // "none" | "bachelor" | "master" | "phd"

    // ---- Job description synthesis ----
    private String summary;                    // 1-2 sentence role summary
    private List<String> responsibilities;     // key bullets
    private List<String> keywords;             // for ATS/search

    // ---- Quality signals (mirrors CvAgent scoring) ----
    private Scores scores;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Scores {
        private Integer clarity;         // 0-100: how clear and specific is the JD
        private Integer specificity;     // 0-100: are requirements concrete?
        private Integer inclusivity;     // 0-100: neutral, welcoming language
        private Integer overall;         // 0-100 aggregate
    }
}