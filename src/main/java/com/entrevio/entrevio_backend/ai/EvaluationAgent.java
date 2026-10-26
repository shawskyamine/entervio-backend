package com.entrevio.entrevio_backend.ai;

import com.entrevio.entrevio_backend.dto.cv.ParsedCv;
import com.entrevio.entrevio_backend.dto.job.ParsedJob;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EvaluationAgent {

    private final ChatClient chatClient;

    public EvaluationAgent(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    /**
     * Writes a 2-3 sentence explanation for why this candidate fits (or doesn't)
     * this job. The score has already been computed deterministically — this
     * method only translates that result into natural language.
     */
    public String explain(
            ParsedJob job,
            ParsedCv cv,
            int score,
            List<String> matchedSkills,
            List<String> missingSkills
    ) {
        String systemPrompt = """
            You are a hiring analyst writing a brief, neutral explanation
            for a recruiter about why a candidate matches (or doesn't match)
            a job.

            You are NOT scoring. A score has already been computed by
            deterministic code. Your job is to explain it in 2-3 sentences.

            Rules:
            - Refer to concrete strengths and gaps using the provided data.
            - Mention the score directly if it's useful context.
            - Be honest about weaknesses — recruiters need signal, not flattery.
            - Do NOT invent facts. Only use what's provided.
            - Do NOT use emojis, exclamation marks, or marketing language.
            - Maximum 60 words.
            - Write in plain English, third person ("The candidate…").
            """;

        String userMessage = String.format("""
            Job title: %s
            Job requires: %s
            Job min years: %s
            Job seniority: %s
            Job education: %s

            Candidate: %s
            Candidate headline: %s
            Candidate years: %s
            Candidate skills: %s

            Deterministic match score: %d / 100

            Skills the candidate HAS that the job wants: %s
            Skills the candidate LACKS that the job wants: %s

            Write the explanation now.
            """,
                nullSafe(job.getSummary()),
                join(job.getRequiredSkills()),
                job.getMinYearsExperience(),
                job.getSeniority(),
                job.getEducationLevel(),

                nullSafe(cv.getHeadline()),
                nullSafe(cv.getHeadline()),
                cv.getYearsExperience(),
                join(cv.getSkills()),

                score,
                join(matchedSkills),
                join(missingSkills)
        );

        return chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .content();
    }

    private String join(List<String> list) {
        if (list == null || list.isEmpty()) return "(none)";
        return String.join(", ", list);
    }

    private String nullSafe(String s) {
        return s == null ? "(not set)" : s;
    }
}