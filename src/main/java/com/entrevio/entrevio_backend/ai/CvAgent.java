package com.entrevio.entrevio_backend.ai;

import com.entrevio.entrevio_backend.dto.cv.ParsedCv;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class CvAgent {

    private final ChatClient chatClient;

    public CvAgent(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public ParsedCv parse(String rawCvText) {
        String systemPrompt = """
            You are an expert CV analyst and ATS (Applicant Tracking System) reviewer.
            You receive raw text extracted from a candidate's PDF CV.

            You must return ONLY valid JSON, with no prose, no markdown fences, no explanation.

            The JSON MUST match this exact schema:
            {
              "skills": ["string"],
              "yearsExperience": number,
              "headline": "string",
              "education": [
                { "degree": "string", "institution": "string", "year": "string" }
              ],
              "scores": {
                "atsFriendly": number,
                "clarity": number,
                "impact": number,
                "completeness": number,
                "overall": number
              },
              "improvements": [
                {
                  "area": "string",
                  "issue": "string",
                  "suggestion": "string",
                  "severity": "high" | "medium" | "low"
                }
              ]
            }

            Rules for extraction:
            - "skills": concise list of technical and soft skills.
            - "yearsExperience": integer. If unknown, use 0.
            - "headline": one short sentence summarising the candidate (max 120 chars).
            - "education": only degrees/diplomas. Empty array if none.

            Rules for scoring (each value is an integer 0-100):
            - "atsFriendly": can standard ATS software parse this CV cleanly?
              Deduct for: tables, columns, graphics, images of text, unusual fonts,
              missing section headers (Experience, Education, Skills), missing dates,
              missing contact info.
            - "clarity": how easy is it for a human recruiter to skim?
              Deduct for: long unbroken paragraphs, vague job titles, jargon overload,
              inconsistent formatting, missing bullet points.
            - "impact": how well does the CV demonstrate results?
              Deduct for: duties listed without achievements, no numbers/metrics,
              no action verbs, passive voice, generic statements ("hard worker").
            - "completeness": are all standard sections present?
              Check: contact info, professional summary, work experience with dates,
              education, skills. Deduct ~20 per missing section.
            - "overall": weighted average (ATS 30%%, clarity 20%%, impact 30%%, completeness 20%%).

            Rules for improvements:
            - Provide between 3 and 6 items, ordered by importance.
            - "area": short label, e.g. "ATS Compatibility", "Quantify achievements".
            - "issue": one sentence describing what is wrong or missing.
            - "suggestion": one concrete action the candidate should take.
            - "severity": "high" if it materially hurts hiring chances, "medium" if it's a real
              but minor issue, "low" if it's polish.

            Never invent information that is not in the CV.
            """;

        return chatClient.prompt()
                .system(systemPrompt)
                .user(u -> u.text("CV text:\n\n{text}").param("text", rawCvText))
                .call()
                .entity(ParsedCv.class);
    }
}