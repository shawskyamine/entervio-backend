package com.entrevio.entrevio_backend.ai;

import com.entrevio.entrevio_backend.dto.job.ParsedJob;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class JobAgent {

    private final ChatClient chatClient;

    public JobAgent(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public ParsedJob parse(String title, String description) {
        String systemPrompt = """
            You are an expert technical recruiter and job description analyst.

            You receive a job title and a raw job description. You must extract
            structured information and return ONLY valid JSON, with no prose,
            no markdown fences, no explanation.

            The JSON MUST match this exact schema:
            {
              "requiredSkills": ["string"],
              "preferredSkills": ["string"],
              "minYearsExperience": number,
              "seniority": "junior" | "mid" | "senior" | "lead",
              "educationLevel": "none" | "bachelor" | "master" | "phd",
              "summary": "string",
              "responsibilities": ["string"],
              "keywords": ["string"],
              "scores": {
                "clarity": number,
                "specificity": number,
                "inclusivity": number,
                "overall": number
              }
            }

            Extraction rules:
            - "requiredSkills": the must-have technical and domain skills.
              Be precise — use canonical names like "Java", "Spring Boot",
              "PostgreSQL", "REST APIs" instead of vague phrases.
            - "preferredSkills": nice-to-haves, or skills marked "bonus" / "plus".
            - "minYearsExperience": integer. If the JD says "3+ years", return 3.
              If unclear, estimate from seniority: junior=0, mid=3, senior=5, lead=8.
            - "seniority": infer from title and requirements.
            - "educationLevel": the minimum required. If not specified, "none".
            - "summary": 1-2 sentences summarising the role. Max 200 chars.
            - "responsibilities": 4-8 short bullets from the JD body.
            - "keywords": 5-12 terms for ATS/search indexing.

            Scoring rules (each 0-100, integer):
            - "clarity": is the role well-defined? Deduct for vague titles,
              missing scope, buzzword soup.
            - "specificity": are requirements concrete? Deduct for phrases like
              "rockstar", "ninja", "fast-paced environment" with no substance.
            - "inclusivity": is the language neutral and welcoming? Deduct for
              gendered terms, "young/dynamic team", culture-fit clichés.
            - "overall": weighted average (clarity 40%, specificity 40%, inclusivity 20%).

            Never invent information not present in the JD.
            """;

        String userMessage = "Title: " + title + "\n\nDescription:\n" + description;

        return chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .entity(ParsedJob.class);
    }
}