package com.entrevio.entrevio_backend.dto.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CvRagSearchResponse {
    private String answer;
    private List<Citation> citations;
    private int totalChunksSearched;
    private long searchTimeMs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Citation {
        private int ref;
        private UUID candidateId;
        private String candidateName;
        private String candidateEmail;
        private String headline;
        private UUID cvId;
        private UUID chunkId;
        /** Legacy plain-text snippet (kept for backwards compatibility). */
        private String snippet;
        private double similarity;
        /** Parsed skills array from the SKILLS chunk. */
        private List<String> skills;
        /** Relevant experience — RAW chunks only, no HEADER/SKILLS. */
        private List<EvidenceChunk> evidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvidenceChunk {
        private String content;
        private double similarity;
        private int tokenCount;
    }
}