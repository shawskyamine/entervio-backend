package com.entrevio.entrevio_backend.dto.interview;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewPrepResponse {
    private String candidateName;
    private String jobTitle;
    private String intro;
    private String openingLine;
    private List<Question> questions;
    private List<String> redFlags;
    private List<String> strengthsToConfirm;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Question {
        private String question;
        private String reason;
        private String category;   // "technical" | "behavioral" | "red_flag" | "strength_confirmation"
    }
}