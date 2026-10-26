package com.entrevio.entrevio_backend.dto.cv;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedCv {

    // ---- Existing fields ----
    private List<String> skills;
    private Integer yearsExperience;
    private String headline;
    private List<Education> education;

    // ---- New: scoring ----
    private Scores scores;
    private List<Improvement> improvements;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Education {
        private String degree;
        private String institution;
        private String year;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Scores {
        private Integer atsFriendly;     // 0-100
        private Integer clarity;         // 0-100
        private Integer impact;          // 0-100
        private Integer completeness;    // 0-100
        private Integer overall;         // 0-100 (aggregate)
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Improvement {
        private String area;         // e.g. "Contact Information"
        private String issue;        // what's wrong / missing
        private String suggestion;   // what to do
        private String severity;     // "high" | "medium" | "low"
    }
}