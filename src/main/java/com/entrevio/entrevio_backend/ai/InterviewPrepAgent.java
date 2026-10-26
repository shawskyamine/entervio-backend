package com.entrevio.entrevio_backend.ai;

import com.entrevio.entrevio_backend.dto.interview.InterviewPrepResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class InterviewPrepAgent {

    private final ChatClient chatClient;

    public InterviewPrepAgent(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public InterviewPrepResponse generate(String context) {
        String systemPrompt = """
            You are a senior recruiter preparing for an interview.
            You have been given:
            - the candidate's parsed CV
            - the job's parsed requirements
            - the deterministic match score and breakdown
            - the candidate's application cover note (if any)

            Your task: produce a concise interview preparation document.

            You MUST return ONLY valid JSON matching this exact schema:
            {
              "intro": "1-2 sentence overall assessment (max 200 chars)",
              "openingLine": "1 sentence to open the interview, grounded in the CV (max 150 chars)",
              "questions": [
                {
                  "question": "the actual question to ask",
                  "reason": "why this question matters for this candidate",
                  "category": "technical" | "behavioral" | "red_flag" | "strength_confirmation"
                }
              ],
              "redFlags": ["specific things to probe (gaps, contradictions, weak areas)"],
              "strengthsToConfirm": ["claims worth validating"]
            }

            Rules for questions:
            - Produce 5 to 8 questions total.
            - 2-4 technical questions tied to the required skills and any gaps.
            - 1-2 behavioral questions (team, leadership, conflict) if relevant.
            - 1-3 red_flag questions based on CV gaps, short tenures, or mismatches with the job.
            - 1-2 strength_confirmation questions for claims that need verification.
            - Each "reason" must be specific — reference actual CV or job content.
            - Do NOT ask generic questions like "tell me about yourself".
            - Do NOT repeat the same question in different forms.

            Rules for redFlags and strengthsToConfirm:
            - 1-4 items each, short bullets.
            - Empty array if genuinely nothing to flag.

            Do NOT invent facts. Only use what is in the provided context.
            """;

        return chatClient.prompt()
                .system(systemPrompt)
                .user(context)
                .call()
                .entity(InterviewPrepResponse.class);
    }
}