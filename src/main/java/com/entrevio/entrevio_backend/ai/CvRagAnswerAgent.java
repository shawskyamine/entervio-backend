package com.entrevio.entrevio_backend.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CvRagAnswerAgent {

    private final ChatClient chatClient;

    public CvRagAnswerAgent(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String answer(String query, List<Snippet> snippets) {
        String systemPrompt = """
            You are a recruitment analyst. A recruiter has asked a question
            about candidates in their talent pool. You have been given excerpts
            from candidate CVs that were retrieved as semantically relevant.

            Your job:
            1. Read the excerpts.
            2. Decide which candidates genuinely match the query.
            3. Write a concise answer (3-6 sentences) that:
               - Lists matching candidates by NAME
               - Explains briefly WHY each matches (using the excerpt content)
               - Cites each candidate with a bracketed number [1], [2], ...
                 that corresponds to the excerpt order you were given.
               - Says "no strong matches" if none of the excerpts are relevant.

            Rules:
            - Only use the provided excerpts. Do not invent facts.
            - Treat equivalent technologies as matching (e.g. J2EE ≡ Java EE ≡ Java).
            - Do NOT mention chunk IDs, similarity scores, or that this is a RAG system.
            - Do NOT use emojis.
            - Do NOT open with "Based on the excerpts…" — just answer directly.
            """;

        StringBuilder userMessage = new StringBuilder();
        userMessage.append("Recruiter's question:\n").append(query).append("\n\n");
        userMessage.append("Retrieved CV excerpts:\n\n");

        for (int i = 0; i < snippets.size(); i++) {
            Snippet s = snippets.get(i);
            userMessage.append("[").append(i + 1).append("] ")
                    .append(s.candidateName())
                    .append(s.headline() != null ? " — " + s.headline() : "")
                    .append("\n")
                    .append(s.content())
                    .append("\n\n");
        }

        userMessage.append("Now write the answer.");

        return chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage.toString())
                .call()
                .content();
    }

    public record Snippet(String candidateName, String headline, String content) {}
}